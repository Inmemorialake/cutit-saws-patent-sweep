package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.NotificacionesClient;
import co.edu.univalle.patentsweep.integration.dto.NotificacionRequestDTO;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Notificacion;
import co.edu.univalle.patentsweep.model.enums.EstadoNotificacion;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.NotificacionRepository;
import co.edu.univalle.patentsweep.service.event.ConflictoDetectadoEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 27, 15, 0, 0);

    @Mock
    private NotificacionesClient notificacionesClient;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private ConflictoRepository conflictoRepository;

    private NotificacionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new NotificacionService(notificacionesClient, notificacionRepository, conflictoRepository, RELOJ);
    }

    @Test
    @DisplayName("Un conflicto nuevo se notifica con el contrato del PB y queda NOTIFICADO")
    void deberiaEnviarLaAlertaYMarcarNotificado() {
        // Arrange
        ConflictoDetectadoEvent evento = new ConflictoDetectadoEvent(100001L, "form-001", "PAT-998822");
        when(notificacionRepository.findByConflicto_ConflictoId(100001L)).thenReturn(Optional.empty());
        when(conflictoRepository.getReferenceById(100001L)).thenReturn(Conflicto.builder().conflictoId(100001L).build());

        // Act
        servicio.alDetectarConflicto(evento);

        // Assert
        ArgumentCaptor<NotificacionRequestDTO> enviada = ArgumentCaptor.forClass(NotificacionRequestDTO.class);
        verify(notificacionesClient).enviar(enviada.capture());
        assertThat(enviada.getValue().conflictoId()).isEqualTo("100001");
        assertThat(enviada.getValue().formulaId()).isEqualTo("form-001");
        assertThat(enviada.getValue().patenteId()).isEqualTo("PAT-998822");
        assertThat(enviada.getValue().tipoAlerta()).isEqualTo("CONFLICTO_PATENTE_DETECTADO");
        assertThat(enviada.getValue().mensaje())
                .isEqualTo("Se ha identificado un conflicto de patente en la fórmula form-001.");

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoNotificacion.NOTIFICADO);
        assertThat(guardada.getValue().getFechaEnvio()).isEqualTo(AHORA);
        assertThat(guardada.getValue().getIntentos()).isEqualTo(1);
        assertThat(guardada.getValue().getMensajeError()).isNull();
    }

    @Test
    @DisplayName("Un conflicto ya NOTIFICADO no se vuelve a enviar (idempotencia)")
    void noDeberiaReenviarUnConflictoYaNotificado() {
        // Arrange
        Notificacion notificada = Notificacion.builder().estado(EstadoNotificacion.NOTIFICADO).build();
        when(notificacionRepository.findByConflicto_ConflictoId(100001L)).thenReturn(Optional.of(notificada));

        // Act
        servicio.alDetectarConflicto(new ConflictoDetectadoEvent(100001L, "form-001", "PAT-998822"));

        // Assert
        verify(notificacionesClient, never()).enviar(any());
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si Notificaciones falla el conflicto queda PENDIENTE_NOTIFICACION y el error no se propaga")
    void deberiaDejarPendienteCuandoElEnvioFalla() {
        // Arrange
        when(notificacionRepository.findByConflicto_ConflictoId(100001L)).thenReturn(Optional.empty());
        when(conflictoRepository.getReferenceById(100001L)).thenReturn(Conflicto.builder().conflictoId(100001L).build());
        doThrow(new ServicioNoDisponibleException("sin respuesta")).when(notificacionesClient).enviar(any());

        // Act
        servicio.alDetectarConflicto(new ConflictoDetectadoEvent(100001L, "form-001", "PAT-998822"));

        // Assert
        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE_NOTIFICACION);
        assertThat(guardada.getValue().getMensajeError()).isEqualTo("sin respuesta");
        assertThat(guardada.getValue().getFechaEnvio()).isNull();
    }

    @Test
    @DisplayName("Una notificación PENDIENTE se reintenta y, si esta vez funciona, queda NOTIFICADO")
    void deberiaReintentarUnaNotificacionPendiente() {
        // Arrange
        Notificacion pendiente = Notificacion.builder().estado(EstadoNotificacion.PENDIENTE_NOTIFICACION)
                .intentos(1).mensajeError("sin respuesta").build();
        when(notificacionRepository.findByConflicto_ConflictoId(100001L)).thenReturn(Optional.of(pendiente));

        // Act
        servicio.alDetectarConflicto(new ConflictoDetectadoEvent(100001L, "form-001", "PAT-998822"));

        // Assert
        assertThat(pendiente.getEstado()).isEqualTo(EstadoNotificacion.NOTIFICADO);
        assertThat(pendiente.getIntentos()).isEqualTo(2);
        assertThat(pendiente.getMensajeError()).isNull();
        verify(conflictoRepository, never()).getReferenceById(any());
    }

    @Test
    @DisplayName("Sin formulaId no se envía la alerta y el conflicto queda pendiente")
    void noDeberiaEnviarCuandoFaltaLaFormula() {
        // Arrange
        when(notificacionRepository.findByConflicto_ConflictoId(100001L)).thenReturn(Optional.empty());
        when(conflictoRepository.getReferenceById(100001L)).thenReturn(Conflicto.builder().conflictoId(100001L).build());

        // Act
        servicio.alDetectarConflicto(new ConflictoDetectadoEvent(100001L, null, "PAT-998822"));

        // Assert
        verify(notificacionesClient, never()).enviar(any());
        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE_NOTIFICACION);
        assertThat(guardada.getValue().getMensajeError()).isEqualTo("Faltan datos obligatorios para enviar la alerta");
    }

    @Test
    @DisplayName("Sin patenteId, o con valores en blanco, tampoco se envía la alerta")
    void noDeberiaEnviarCuandoFaltaLaPatenteOElValorEstaEnBlanco() {
        // Arrange
        when(notificacionRepository.findByConflicto_ConflictoId(100001L)).thenReturn(Optional.empty());
        when(conflictoRepository.getReferenceById(100001L)).thenReturn(Conflicto.builder().conflictoId(100001L).build());

        // Act
        servicio.alDetectarConflicto(new ConflictoDetectadoEvent(100001L, "form-001", "  "));
        servicio.alDetectarConflicto(new ConflictoDetectadoEvent(100001L, "form-001", null));

        // Assert
        verify(notificacionesClient, never()).enviar(any());
    }
}
