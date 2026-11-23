package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.model.RevisionFormula;
import co.edu.univalle.patentsweep.model.Usuario;
import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.PatenteRepository;
import co.edu.univalle.patentsweep.repository.RevisionFormulaRepository;
import co.edu.univalle.patentsweep.repository.UsuarioRepository;
import co.edu.univalle.patentsweep.service.event.ConflictoDetectadoEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra el resultado de una revisión en una sola transacción: patentes, conflictos y evento del
 * historial. Publica {@link ConflictoDetectadoEvent} por cada conflicto nuevo; el listener lo procesa
 * después del commit (P12). El estado de la fórmula no se guarda: se calcula al consultarlo.
 */
@Service
public class RevisionService {

    private static final String FUENTE_EXTERNA = "FUENTE_EXTERNA";

    private final UsuarioRepository usuarioRepository;
    private final PatenteRepository patenteRepository;
    private final ConflictoRepository conflictoRepository;
    private final RevisionFormulaRepository revisionRepository;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;

    public RevisionService(UsuarioRepository usuarioRepository, PatenteRepository patenteRepository,
                           ConflictoRepository conflictoRepository, RevisionFormulaRepository revisionRepository,
                           ApplicationEventPublisher publicador, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.patenteRepository = patenteRepository;
        this.conflictoRepository = conflictoRepository;
        this.revisionRepository = revisionRepository;
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
        String formulaId = formulaExterna.codigo();
        String version = formulaExterna.versionRegistrada();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        List<Patente> patentes = new ArrayList<>();
        List<Optional<Conflicto>> existentes = new ArrayList<>();
        for (PatenteExternaDTO coincidencia : coincidencias) {
            Optional<Patente> guardada = patenteRepository.findByCodigoPatente(coincidencia.patenteId());
            Patente patente = guardada.orElseGet(() -> patenteRepository.save(aPatente(coincidencia)));
            patentes.add(patente);
            existentes.add(guardada.flatMap(p -> conflictoRepository
                    .findByFormulaIdAndPatente_PatenteIdAndVersionFormula(formulaId, p.getPatenteId(), version)));
        }

        boolean hayNuevos = existentes.stream().anyMatch(Optional::isEmpty);
        boolean hayVigentes = conflictoRepository.countByFormulaIdAndVersionFormulaAndEstado(
                formulaId, version, EstadoConflicto.DETECTADO) > 0;
        ResultadoRevision resultado = hayNuevos || hayVigentes
                ? ResultadoRevision.CON_CONFLICTO : ResultadoRevision.SIN_CONFLICTO;

        RevisionFormula revision = guardarRevision(formulaId, version, username, resultado, parametros, null, ahora);

        List<Conflicto> conflictos = new ArrayList<>();
        for (int i = 0; i < patentes.size(); i++) {
            if (existentes.get(i).isPresent()) {
                conflictos.add(existentes.get(i).get());
            } else {
                Conflicto nuevo = conflictoRepository.save(Conflicto.builder()
                        .revision(revision)
                        .formulaId(formulaId)
                        .patente(patentes.get(i))
                        .versionFormula(version)
                        .estado(EstadoConflicto.DETECTADO)
                        .fechaDeteccion(ahora)
                        .build());
                conflictos.add(nuevo);
                publicador.publishEvent(new ConflictoDetectadoEvent(
                        nuevo.getConflictoId(), formulaId, patentes.get(i).getCodigoPatente()));
            }
        }
        return conflictos;
    }

    /** Registra que la fuente externa falló tras los reintentos (PB HU-02, criterio 7.2). */
    @Transactional
    public void registrarFalloTecnico(FormulaExternaDTO formulaExterna, String username, String mensajeError,
                                      Map<String, Object> parametros) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        guardarRevision(formulaExterna.codigo(), formulaExterna.versionRegistrada(), username,
                ResultadoRevision.FALLO_TECNICO, parametros, mensajeError, ahora);
    }

    private RevisionFormula guardarRevision(String formulaId, String version, String username,
                                            ResultadoRevision resultado, Map<String, Object> parametros,
                                            String mensajeError, LocalDateTime fecha) {
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        return revisionRepository.save(RevisionFormula.builder()
                .formulaId(formulaId)
                .versionFormula(version)
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
