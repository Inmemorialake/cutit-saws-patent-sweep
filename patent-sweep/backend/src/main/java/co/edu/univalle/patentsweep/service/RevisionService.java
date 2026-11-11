package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Formula;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.model.RevisionFormula;
import co.edu.univalle.patentsweep.model.Usuario;
import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
import co.edu.univalle.patentsweep.model.enums.EstadoFormula;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.FormulaRepository;
import co.edu.univalle.patentsweep.repository.PatenteRepository;
import co.edu.univalle.patentsweep.repository.RevisionFormulaRepository;
import co.edu.univalle.patentsweep.repository.UsuarioRepository;
import co.edu.univalle.patentsweep.service.event.ConflictoDetectadoEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra el resultado de una revisión en una sola transacción: fórmula, patentes, conflictos,
 * evento del historial y estado precalculado. Publica {@link ConflictoDetectadoEvent} por cada
 * conflicto nuevo; el listener lo procesa después del commit (P12).
 */
@Service
public class RevisionService {

    private static final String FUENTE_EXTERNA = "FUENTE_EXTERNA";
    private static final String SIN_VERSION = "SIN_VERSION";

    private final FormulaRepository formulaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PatenteRepository patenteRepository;
    private final ConflictoRepository conflictoRepository;
    private final RevisionFormulaRepository revisionRepository;
    private final EstadoFormulaService estadoFormulaService;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;

    public RevisionService(FormulaRepository formulaRepository, UsuarioRepository usuarioRepository,
                           PatenteRepository patenteRepository, ConflictoRepository conflictoRepository,
                           RevisionFormulaRepository revisionRepository, EstadoFormulaService estadoFormulaService,
                           ApplicationEventPublisher publicador, Clock reloj) {
        this.formulaRepository = formulaRepository;
        this.usuarioRepository = usuarioRepository;
        this.patenteRepository = patenteRepository;
        this.conflictoRepository = conflictoRepository;
        this.revisionRepository = revisionRepository;
        this.estadoFormulaService = estadoFormulaService;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    /**
     * Registra una búsqueda manual. Devuelve los conflictos de las patentes coincidentes, nuevos o ya
     * existentes: nunca se duplica la terna fórmula-patente-versión, sin importar su estado (DA-17).
     */
    @Transactional
    public List<Conflicto> registrarBusqueda(FormulaExternaDTO formulaExterna, String username,
                                             List<PatenteExternaDTO> coincidencias,
                                             Map<String, Object> parametros) {
        Formula formula = sincronizarFormula(formulaExterna);
        String version = versionDe(formulaExterna);
        LocalDateTime ahora = LocalDateTime.now(reloj);

        List<Patente> patentes = new ArrayList<>();
        List<Optional<Conflicto>> existentes = new ArrayList<>();
        for (PatenteExternaDTO coincidencia : coincidencias) {
            Optional<Patente> guardada = patenteRepository.findByCodigoPatente(coincidencia.patenteId());
            Patente patente = guardada.orElseGet(() -> patenteRepository.save(aPatente(coincidencia)));
            patentes.add(patente);
            existentes.add(guardada.flatMap(p -> conflictoRepository
                    .findByFormula_FormulaIdAndPatente_PatenteIdAndVersionFormula(
                            formula.getFormulaId(), p.getPatenteId(), version)));
        }

        boolean hayNuevos = existentes.stream().anyMatch(Optional::isEmpty);
        boolean hayVigentes = conflictoRepository.countByFormula_FormulaIdAndVersionFormulaAndEstado(
                formula.getFormulaId(), version, EstadoConflicto.DETECTADO) > 0;
        ResultadoRevision resultado = hayNuevos || hayVigentes
                ? ResultadoRevision.CON_CONFLICTO : ResultadoRevision.SIN_CONFLICTO;

        RevisionFormula revision = guardarRevision(formula, username, resultado, parametros, null, ahora);

        List<Conflicto> conflictos = new ArrayList<>();
        for (int i = 0; i < patentes.size(); i++) {
            if (existentes.get(i).isPresent()) {
                conflictos.add(existentes.get(i).get());
            } else {
                Conflicto nuevo = conflictoRepository.save(Conflicto.builder()
                        .revision(revision)
                        .formula(formula)
                        .patente(patentes.get(i))
                        .versionFormula(version)
                        .estado(EstadoConflicto.DETECTADO)
                        .fechaDeteccion(ahora)
                        .build());
                conflictos.add(nuevo);
                publicador.publishEvent(new ConflictoDetectadoEvent(
                        nuevo.getConflictoId(), formula.getFormulaId(), patentes.get(i).getCodigoPatente()));
            }
        }
        estadoFormulaService.actualizarEstado(formula, version, resultado, ahora);
        return conflictos;
    }

    /** Registra que la fuente externa falló tras los reintentos (PB HU-02, criterio 7.2). */
    @Transactional
    public void registrarFalloTecnico(FormulaExternaDTO formulaExterna, String username, String mensajeError,
                                      Map<String, Object> parametros) {
        Formula formula = sincronizarFormula(formulaExterna);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        guardarRevision(formula, username, ResultadoRevision.FALLO_TECNICO, parametros, mensajeError, ahora);
        estadoFormulaService.actualizarEstado(formula, versionDe(formulaExterna),
                ResultadoRevision.FALLO_TECNICO, ahora);
    }

    private static String versionDe(FormulaExternaDTO formulaExterna) {
        return Objects.toString(formulaExterna.version(), SIN_VERSION);
    }

    /** Crea o actualiza la copia local de la fórmula (la dueña de los datos es Materiales/Fórmulas). */
    private Formula sincronizarFormula(FormulaExternaDTO externa) {
        Formula formula = formulaRepository.findById(externa.codigo()).orElseGet(() -> Formula.builder()
                .formulaId(externa.codigo())
                .estado(EstadoFormula.NO_PROCESADO)
                .build());
        formula.setNombre(externa.nombre());
        formula.setDescripcion(externa.aplicacionTecnica());
        formula.setActivo(!externa.estaDescontinuada());
        return formulaRepository.save(formula);
    }

    private RevisionFormula guardarRevision(Formula formula, String username, ResultadoRevision resultado,
                                            Map<String, Object> parametros, String mensajeError,
                                            LocalDateTime fecha) {
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        return revisionRepository.save(RevisionFormula.builder()
                .formula(formula)
                .usuario(usuario)
                .origen(OrigenRevision.BUSQUEDA_MANUAL)
                .fechaRevision(fecha)
                .resultado(resultado)
                .parametros(parametros)
                .traceId(MDC.get("traceId"))
                .mensajeError(mensajeError)
                .build());
    }

    private static Patente aPatente(PatenteExternaDTO dto) {
        return Patente.builder()
                .codigoPatente(dto.patenteId())
                .titulo(dto.tituloPatente())
                .tipoProducto(dto.tipoProducto())
                .resumen(dto.resumen())
                .reivindicaciones(dto.reivindicaciones() == null ? null : String.join("\n", dto.reivindicaciones()))
                .fechaSolicitud(dto.fechaSolicitud())
                .fechaConcesion(dto.fechaConcesion())
                .fechaVencimiento(dto.fechaVencimiento())
                .jurisdiccion(dto.jurisdiccion())
                .estadoVigencia(dto.estadoPatente())
                .fuente(FUENTE_EXTERNA)
                .build();
    }
}
