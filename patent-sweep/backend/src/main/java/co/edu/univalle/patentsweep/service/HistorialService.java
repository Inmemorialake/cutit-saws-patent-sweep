package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.model.RevisionFormula;
import co.edu.univalle.patentsweep.model.Usuario;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.RevisionFormulaRepository;
import co.edu.univalle.patentsweep.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Historial append-only de revisiones (P18): guarda usuario, parámetros, traceId y resultado. */
@Service
public class HistorialService {

    private final UsuarioRepository usuarioRepository;
    private final RevisionFormulaRepository revisionRepository;

    public HistorialService(UsuarioRepository usuarioRepository, RevisionFormulaRepository revisionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.revisionRepository = revisionRepository;
    }

    @Transactional
    public RevisionFormula registrar(String formulaId, String version, String username,
                                     OrigenRevision origen, ResultadoRevision resultado,
                                     Map<String, Object> parametros, String mensajeError, LocalDateTime fecha) {
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        return revisionRepository.save(RevisionFormula.builder()
                .formulaId(formulaId)
                .versionFormula(version)
                .usuario(usuario)
                .origen(origen)
                .fechaRevision(fecha)
                .resultado(resultado)
                .parametros(parametros)
                .traceId(MDC.get("traceId"))
                .mensajeError(mensajeError)
                .build());
    }
}
