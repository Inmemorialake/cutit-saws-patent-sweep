package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.exception.ConfiguracionNoEncontradaException;
import co.edu.univalle.patentsweep.exception.FormulaDescontinuadaException;
import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.MaterialesClient;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaResumenDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.integration.port.FuentePatentesPort;
import co.edu.univalle.patentsweep.model.Barrido;
import co.edu.univalle.patentsweep.model.Configuracion;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.enums.EstadoBarrido;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.BarridoRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Barrido automático de fórmulas aprobadas. La lógica de conflictos se reutiliza de HU-02, pero la
 * persistencia del historial se marca con origen BARRIDO_AUTOMATICO y el barrido sigue aunque una
 * fórmula individual falle (D-3, ADR-008).
 */
@Service
public class BarridoService {

    private static final Logger LOG = LoggerFactory.getLogger(BarridoService.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String USUARIO_SISTEMA = "SISTEMA";
    private static final String SIN_VERSION = "SIN_VERSION";
    private static final String MENSAJE_SIN_FORMULAS =
            "No hay fórmulas registradas en estado APROBADA para evaluar";
    private static final String MENSAJE_MATERIALES = "No se pudo comunicar con el servicio de Materiales/Fórmulas";
    private static final String MENSAJE_FUENTE = "No se pudo comunicar con la fuente externa de patentes";
    private static final List<String> CLASIFICACIONES_INICIALES = List.of("B27B 17/00", "B24D 3/00");

    private final BarridoRepository barridoRepository;
    private final MaterialesClient materialesClient;
    private final FuentePatentesPort fuentePatentes;
    private final MotorCoincidencias motorCoincidencias;
    private final ConfiguracionService configuracionService;
    private final ConflictoService conflictoService;
    private final HistorialService historialService;
    private final EstadoFormulaService estadoFormulaService;
    private final MeterRegistry meterRegistry;
    private final Clock reloj;

    public BarridoService(BarridoRepository barridoRepository, MaterialesClient materialesClient,
                          FuentePatentesPort fuentePatentes, MotorCoincidencias motorCoincidencias,
                          ConfiguracionService configuracionService, ConflictoService conflictoService,
                          HistorialService historialService, EstadoFormulaService estadoFormulaService,
                          MeterRegistry meterRegistry, Clock reloj) {
        this.barridoRepository = barridoRepository;
        this.materialesClient = materialesClient;
        this.fuentePatentes = fuentePatentes;
        this.motorCoincidencias = motorCoincidencias;
        this.configuracionService = configuracionService;
        this.conflictoService = conflictoService;
        this.historialService = historialService;
        this.estadoFormulaService = estadoFormulaService;
        this.meterRegistry = meterRegistry;
        this.reloj = reloj;
    }

    public Barrido ejecutar() {
        Optional<Barrido> enCurso = barridoRepository.findByEstado(EstadoBarrido.EN_CURSO);
        if (enCurso.isPresent()) {
            LOG.info("Barrido omitido: existe una ejecución en curso iniciada el {}",
                    enCurso.get().getFechaInicio());
            return enCurso.get();
        }

        MDC.put("traceId", UUID.randomUUID().toString());
        Instant inicio = Instant.now(reloj);
        Barrido barrido = barridoRepository.save(Barrido.builder()
                .estado(EstadoBarrido.EN_CURSO)
                .fechaInicio(LocalDateTime.now(reloj))
                .build());
        LOG.info("Iniciando barrido automático {}", FORMATO_FECHA.format(barrido.getFechaInicio()));
        try {
            Barrido resultado = ejecutarBarrido(barrido);
            registrarDuracion(inicio);
            return resultado;
        } catch (RuntimeException excepcion) {
            LOG.error("Fallo general del barrido: {}", excepcion.getMessage());
            Barrido fallido = fallar(barrido, "Fallo general del barrido: " + excepcion.getMessage());
            registrarDuracion(inicio);
            return fallido;
        } finally {
            MDC.remove("traceId");
        }
    }

    private Barrido ejecutarBarrido(Barrido barrido) {
        List<FormulaResumenDTO> formulas;
        try {
            formulas = materialesClient.listarAprobadas();
        } catch (IntegracionExternaException excepcion) {
            LOG.error("{}: {}", MENSAJE_MATERIALES, excepcion.getMessage());
            return fallar(barrido, excepcion);
        } catch (ServicioNoDisponibleException excepcion) {
            LOG.error(MENSAJE_MATERIALES);
            return fallar(barrido, MENSAJE_MATERIALES);
        }

        barrido.setTotalFormulas(formulas.size());
        if (formulas.isEmpty()) {
            LOG.info(MENSAJE_SIN_FORMULAS);
            return finalizar(barrido, 0, 0, 0, 0, 0, MENSAJE_SIN_FORMULAS);
        }

        Configuracion configuracion;
        try {
            configuracion = configuracionService.criteriosVigentes();
        } catch (ConfiguracionNoEncontradaException excepcion) {
            LOG.warn("No hay configuración vigente; se aplican los criterios iniciales de HU-01/HU-10");
            configuracion = criteriosIniciales();
        }

        List<String> clasificaciones = Optional.ofNullable(configuracion.getClasificaciones())
                .orElse(CLASIFICACIONES_INICIALES);
        LocalDate fechaDesde = fechaDesde(configuracion);
        int procesadas = 0;
        int conConflicto = 0;
        int sinConflicto = 0;
        int noProcesadas = 0;

        for (FormulaResumenDTO resumen : formulas) {
            String formulaId = resumen.codigo();
            FormulaExternaDTO formula;
            try {
                formula = materialesClient.obtenerFormula(formulaId);
                if (formula.estaDescontinuada()) {
                    throw new FormulaDescontinuadaException();
                }
            } catch (RuntimeException excepcion) {
                LOG.error("No se pudo obtener el detalle de {}: {}", formulaId, excepcion.getMessage());
                registrarFallo(formulaId, SIN_VERSION, Map.of("formula", formulaId), excepcion.getMessage());
                noProcesadas++;
                continue;
            }

            List<String> terminos = motorCoincidencias.terminosDeBusqueda(formula);
            List<PatenteExternaDTO> candidatas;
            try {
                candidatas = fuentePatentes.buscarPatentes(terminos, clasificaciones, fechaDesde);
            } catch (RuntimeException excepcion) {
                LOG.error("{} (fórmula {}): {}", MENSAJE_FUENTE, formulaId, excepcion.getMessage());
                registrarFallo(formulaId, formula.versionRegistrada(),
                        parametros(formula, terminos, clasificaciones, fechaDesde, List.of()), excepcion.getMessage());
                noProcesadas++;
                continue;
            }

            Map<String, Object> parametros =
                    parametros(formula, terminos, clasificaciones, fechaDesde, candidatas);
            List<Conflicto> conflictos;
            try {
                List<PatenteExternaDTO> coincidencias = motorCoincidencias.filtrarConflictos(formula, candidatas);
                conflictos = conflictoService.registrarNuevos(formula, USUARIO_SISTEMA,
                        OrigenRevision.BARRIDO_AUTOMATICO, coincidencias, parametros);
            } catch (RuntimeException excepcion) {
                LOG.error("No se pudo registrar el resultado de {}: {}", formulaId, excepcion.getMessage());
                registrarFallo(formulaId, formula.versionRegistrada(), parametros, excepcion.getMessage());
                noProcesadas++;
                continue;
            }

            procesadas++;
            if (conflictos.isEmpty()) {
                sinConflicto++;
                registrarMetrica("sin_conflicto");
            } else {
                conConflicto++;
                registrarMetrica("con_conflicto");
            }
            try {
                estadoFormulaService.recalcular(formulaId);
            } catch (RuntimeException excepcion) {
                LOG.debug("No se pudo recalcular el estado de {}: {}", formulaId, excepcion.getMessage());
            }
        }

        String mensaje = "Barrido completado exitosamente";
        LOG.info("Barrido finalizado: {} procesadas, {} no procesadas",
                procesadas, noProcesadas);
        return finalizar(barrido, formulas.size(), procesadas, conConflicto, sinConflicto, noProcesadas, mensaje);
    }

    private Configuracion criteriosIniciales() {
        return Configuracion.builder()
                .frecuenciaValor(1)
                .frecuenciaUnidad("DIAS")
                .frecuenciaHoras(24)
                .rangoCantidad(5)
                .rangoUnidad("ANOS")
                .clasificaciones(CLASIFICACIONES_INICIALES)
                .activa(true)
                .vigenteDesde(LocalDateTime.now(reloj))
                .build();
    }

    private LocalDate fechaDesde(Configuracion configuracion) {
        long cantidad = Optional.ofNullable(configuracion.getRangoCantidad()).orElse(5);
        String unidad = Optional.ofNullable(configuracion.getRangoUnidad())
                .orElse("ANOS").toUpperCase(Locale.ROOT);
        LocalDate hoy = LocalDate.now(reloj);
        return switch (unidad) {
            case "DIAS" -> hoy.minusDays(cantidad);
            case "MESES" -> hoy.minusMonths(cantidad);
            case "ANOS" -> hoy.minusYears(cantidad);
            default -> hoy.minusYears(5);
        };
    }

    private static Map<String, Object> parametros(FormulaExternaDTO formula, List<String> terminos,
                                                   List<String> clasificaciones, LocalDate fechaDesde,
                                                   List<PatenteExternaDTO> candidatas) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("fotoFormula", formula);
        parametros.put("terminos", terminos);
        parametros.put("clasificaciones", clasificaciones);
        parametros.put("fechaSolicitudDesde", fechaDesde);
        parametros.put("patentesCandidatas", candidatas);
        return parametros;
    }

    private void registrarFallo(String formulaId, String version, Map<String, Object> parametros, String mensaje) {
        registrarMetrica("no_procesado");
        try {
            historialService.registrar(formulaId, version, USUARIO_SISTEMA, OrigenRevision.BARRIDO_AUTOMATICO,
                    ResultadoRevision.FALLO_TECNICO, parametros, mensaje, LocalDateTime.now(reloj));
        } catch (RuntimeException excepcion) {
            LOG.error("No se pudo registrar el fallo técnico de {}: {}", formulaId, excepcion.getMessage());
        }
        try {
            estadoFormulaService.recalcular(formulaId);
        } catch (RuntimeException excepcion) {
            LOG.debug("No se pudo recalcular el estado de {} tras el fallo: {}",
                    formulaId, excepcion.getMessage());
        }
    }

    private void registrarMetrica(String resultado) {
        Counter.builder("patentsweep.barrido.formulas")
                .description("Fórmulas evaluadas por el barrido automático, por resultado")
                .tag("resultado", resultado)
                .register(meterRegistry)
                .increment();
    }

    private void registrarDuracion(Instant inicio) {
        Timer.builder("patentsweep.barrido.duracion")
                .description("Duración del barrido automático de fórmulas aprobadas")
                .register(meterRegistry)
                .record(Duration.between(inicio, Instant.now(reloj)));
    }

    private Barrido finalizar(Barrido barrido, int totalFormulas, int procesadas, int conConflicto,
                              int sinConflicto, int noProcesadas, String mensaje) {
        barrido.setTotalFormulas(totalFormulas);
        barrido.setTotalProcesadas(procesadas);
        barrido.setTotalConConflicto(conConflicto);
        barrido.setTotalSinConflicto(sinConflicto);
        barrido.setNoProcesadas(noProcesadas);
        barrido.setMensaje(mensaje);
        barrido.setEstado(EstadoBarrido.COMPLETADO);
        barrido.setFechaFin(LocalDateTime.now(reloj));
        return barridoRepository.save(barrido);
    }

    private Barrido fallar(Barrido barrido, RuntimeException excepcion) {
        return fallar(barrido, excepcion.getMessage());
    }

    private Barrido fallar(Barrido barrido, String mensaje) {
        barrido.setTotalFormulas(0);
        barrido.setTotalProcesadas(0);
        barrido.setTotalConConflicto(0);
        barrido.setTotalSinConflicto(0);
        barrido.setNoProcesadas(0);
        barrido.setMensaje(mensaje);
        barrido.setEstado(EstadoBarrido.FALLIDO);
        barrido.setFechaFin(LocalDateTime.now(reloj));
        return barridoRepository.save(barrido);
    }
}
