package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.model.RevisionFormula;
import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.PatenteRepository;
import co.edu.univalle.patentsweep.service.event.ConflictoDetectadoEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro idempotente de conflictos por pareja fórmula-patente (P13, ADR-009). */
@Service
public class ConflictoService {

    private static final String FUENTE_EXTERNA = "FUENTE_EXTERNA";

    private final PatenteRepository patenteRepository;
    private final ConflictoRepository conflictoRepository;
    private final HistorialService historialService;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;

    public ConflictoService(PatenteRepository patenteRepository, ConflictoRepository conflictoRepository,
                            HistorialService historialService, ApplicationEventPublisher publicador, Clock reloj) {
        this.patenteRepository = patenteRepository;
        this.conflictoRepository = conflictoRepository;
        this.historialService = historialService;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    @Transactional
    public List<Conflicto> registrarNuevos(FormulaExternaDTO formulaExterna, String username,
                                            List<PatenteExternaDTO> coincidencias,
                                            Map<String, Object> parametros) {
        return registrarNuevos(formulaExterna, username, OrigenRevision.BUSQUEDA_MANUAL, coincidencias, parametros);
    }

    @Transactional
    public List<Conflicto> registrarNuevos(FormulaExternaDTO formulaExterna, String username,
                                            OrigenRevision origen, List<PatenteExternaDTO> coincidencias,
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
                    .findByFormulaIdAndPatente_PatenteId(formulaId, p.getPatenteId())));
        }

        boolean hayConflictos = !existentes.isEmpty();
        boolean hayVigentes = conflictoRepository.countByFormulaIdAndEstado(formulaId, EstadoConflicto.DETECTADO) > 0;
        ResultadoRevision resultado = hayConflictos || hayVigentes
                ? ResultadoRevision.CON_CONFLICTO : ResultadoRevision.SIN_CONFLICTO;
        RevisionFormula revision = historialService.registrar(formulaId, version, username,
                origen, resultado, parametros, null, ahora);

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
