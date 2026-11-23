package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.exception.FormulaDescontinuadaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.MaterialesClient;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.integration.port.FuentePatentesPort;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Configuracion;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Orquesta la búsqueda manual de HU-02 sin retener conexiones durante las llamadas HTTP. */
@Service
public class BusquedaService {

    private static final String UNIDAD_MESES = "MESES";

    private final MaterialesClient materialesClient;
    private final FuentePatentesPort fuentePatentes;
    private final MotorCoincidencias motorCoincidencias;
    private final ConfiguracionService configuracionService;
    private final ConflictoService conflictoService;
    private final HistorialService historialService;
    private final EstadoFormulaService estadoFormulaService;
    private final Clock reloj;

    public BusquedaService(MaterialesClient materialesClient, FuentePatentesPort fuentePatentes,
                           MotorCoincidencias motorCoincidencias, ConfiguracionService configuracionService,
                           ConflictoService conflictoService, HistorialService historialService,
                           EstadoFormulaService estadoFormulaService, Clock reloj) {
        this.materialesClient = materialesClient;
        this.fuentePatentes = fuentePatentes;
        this.motorCoincidencias = motorCoincidencias;
        this.configuracionService = configuracionService;
        this.conflictoService = conflictoService;
        this.historialService = historialService;
        this.estadoFormulaService = estadoFormulaService;
        this.reloj = reloj;
    }

    public List<Conflicto> buscar(String formulaId, String username) {
        FormulaExternaDTO formula = materialesClient.obtenerFormula(formulaId);
        if (formula.estaDescontinuada()) {
            throw new FormulaDescontinuadaException();
        }
        Configuracion configuracion = configuracionService.criteriosVigentes();
        LocalDate desde = calcularFechaDesde(configuracion);
        List<String> terminos = motorCoincidencias.terminosDeBusqueda(formula);
        Map<String, Object> parametros = armarParametros(configuracion, desde, formula, terminos);

        List<PatenteExternaDTO> candidatas;
        try {
            candidatas = fuentePatentes.buscarPatentes(terminos, configuracion.getClasificaciones(), desde);
        } catch (RuntimeException e) {
            historialService.registrar(formula.codigo(), formula.versionRegistrada(), username,
                    OrigenRevision.BUSQUEDA_MANUAL, ResultadoRevision.FALLO_TECNICO, parametros,
                    e.getMessage(), LocalDateTime.now(reloj));
            throw new ServicioNoDisponibleException("No se pudo comunicar con la fuente externa de patentes", e);
        }

        parametros.put("patentesCandidatas", candidatas.stream().map(PatenteExternaDTO::patenteId).toList());
        List<PatenteExternaDTO> coincidencias = motorCoincidencias.filtrarConflictos(formula, candidatas);
        List<Conflicto> conflictos = conflictoService.registrarNuevos(formula, username, coincidencias, parametros);
        estadoFormulaService.recalcular(formulaId);
        return conflictos;
    }

    private LocalDate calcularFechaDesde(Configuracion configuracion) {
        if (configuracion.getRangoCantidad() == null || configuracion.getRangoUnidad() == null) {
            return null;
        }
        LocalDate hoy = LocalDate.now(reloj);
        return UNIDAD_MESES.equals(configuracion.getRangoUnidad())
                ? hoy.minusMonths(configuracion.getRangoCantidad())
                : hoy.minusYears(configuracion.getRangoCantidad());
    }

    private static Map<String, Object> armarParametros(Configuracion configuracion, LocalDate desde,
                                                       FormulaExternaDTO formula, List<String> terminos) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("formula", formula.foto());
        parametros.put("terminosBusqueda", terminos);
        parametros.put("clasificaciones", configuracion.getClasificaciones());
        parametros.put("fechaSolicitudDesde", desde == null ? null : desde.toString());
        return parametros;
    }
}
