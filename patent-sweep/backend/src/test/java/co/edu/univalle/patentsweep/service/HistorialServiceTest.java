package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.model.RevisionFormula;
import co.edu.univalle.patentsweep.model.Usuario;
import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import co.edu.univalle.patentsweep.repository.RevisionFormulaRepository;
import co.edu.univalle.patentsweep.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class HistorialServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RevisionFormulaRepository revisionRepository;

    @InjectMocks
    private HistorialService servicio;

    @BeforeEach
    void ponerTraceId() {
        MDC.put("traceId", "traza-hu02");
    }

    @AfterEach
    void limpiarTraceId() {
        MDC.clear();
    }

    @Test
    @DisplayName("guarda la revisión con usuario, parámetros y traceId")
    void deberiaGuardarRevisionConUsuario() {
        Usuario usuario = Usuario.builder().username("ingeniera").activo(true).build();
        when(usuarioRepository.findByUsername("ingeniera")).thenReturn(Optional.of(usuario));
        when(revisionRepository.save(any(RevisionFormula.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 27, 15, 0);

        RevisionFormula revision = servicio.registrar("form-001", "2", "ingeniera",
                OrigenRevision.BUSQUEDA_MANUAL, ResultadoRevision.CON_CONFLICTO,
                Map.of("terminosBusqueda", java.util.List.of("Hierro")), null, fecha);

        ArgumentCaptor<RevisionFormula> captor = ArgumentCaptor.forClass(RevisionFormula.class);
        verify(revisionRepository).save(captor.capture());
        assertThat(revision.getUsuario()).isSameAs(usuario);
        assertThat(revision.getTraceId()).isEqualTo("traza-hu02");
        assertThat(revision.getResultado()).isEqualTo(ResultadoRevision.CON_CONFLICTO);
        assertThat(revision.getFechaRevision()).isEqualTo(fecha);
    }

    @Test
    @DisplayName("una revisión del sistema se guarda sin usuario")
    void deberiaGuardarRevisionSinUsuario() {
        when(usuarioRepository.findByUsername("sistema")).thenReturn(Optional.empty());
        when(revisionRepository.save(any(RevisionFormula.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        RevisionFormula revision = servicio.registrar("form-001", "2", "sistema",
                OrigenRevision.BUSQUEDA_MANUAL, ResultadoRevision.FALLO_TECNICO,
                Map.of(), "sin respuesta", LocalDateTime.now());

        assertThat(revision.getUsuario()).isNull();
        assertThat(revision.getMensajeError()).isEqualTo("sin respuesta");
    }
}
