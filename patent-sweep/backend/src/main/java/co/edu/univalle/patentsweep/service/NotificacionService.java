package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.config.AsyncConfig;
import co.edu.univalle.patentsweep.integration.NotificacionesClient;
import co.edu.univalle.patentsweep.integration.dto.NotificacionRequestDTO;
import co.edu.univalle.patentsweep.model.Notificacion;
import co.edu.univalle.patentsweep.model.enums.EstadoNotificacion;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.NotificacionRepository;
import co.edu.univalle.patentsweep.service.event.ConflictoDetectadoEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Despacho asíncrono de alertas (PB HU-12, P12): corre después del commit y en otro hilo, de modo
 * que un fallo de la API de Notificaciones nunca altera ni retrasa la búsqueda que originó el
 * conflicto. Envía una sola notificación por conflicto (P13); si falla, queda PENDIENTE_NOTIFICACION.
 */
@Service
public class NotificacionService {

    private static final Logger REGISTRO = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionesClient notificacionesClient;
    private final NotificacionRepository notificacionRepository;
    private final ConflictoRepository conflictoRepository;
    private final Clock reloj;

    public NotificacionService(NotificacionesClient notificacionesClient,
                               NotificacionRepository notificacionRepository,
                               ConflictoRepository conflictoRepository, Clock reloj) {
        this.notificacionesClient = notificacionesClient;
        this.notificacionRepository = notificacionRepository;
        this.conflictoRepository = conflictoRepository;
        this.reloj = reloj;
    }

    @Async(AsyncConfig.EJECUTOR_NOTIFICACIONES)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alDetectarConflicto(ConflictoDetectadoEvent evento) {
        Optional<Notificacion> existente = notificacionRepository.findByConflicto_ConflictoId(evento.conflictoId());
        if (existente.isPresent() && existente.get().getEstado() == EstadoNotificacion.NOTIFICADO) {
            return;
        }
        Notificacion notificacion = existente.orElseGet(() -> nuevaNotificacion(evento));

        if (esVacio(evento.formulaId()) || esVacio(evento.patenteId())) {
            REGISTRO.error("Alerta no enviada: faltan formulaId o patenteId del conflicto {}", evento.conflictoId());
            notificacion.setMensajeError("Faltan datos obligatorios para enviar la alerta");
            notificacionRepository.save(notificacion);
            return;
        }

        notificacion.setIntentos(notificacion.getIntentos() + 1);
        try {
            notificacionesClient.enviar(new NotificacionRequestDTO(
                    String.valueOf(evento.conflictoId()), evento.formulaId(), evento.patenteId(),
                    NotificacionRequestDTO.TIPO_ALERTA,
                    "Se ha identificado un conflicto de patente en la fórmula " + evento.formulaId() + "."));
            notificacion.setEstado(EstadoNotificacion.NOTIFICADO);
            notificacion.setFechaEnvio(LocalDateTime.now(reloj));
            notificacion.setMensajeError(null);
        } catch (RuntimeException e) {
            REGISTRO.error("No se pudo comunicar con el servicio de Notificaciones: {}", e.getMessage());
            notificacion.setEstado(EstadoNotificacion.PENDIENTE_NOTIFICACION);
            notificacion.setMensajeError(e.getMessage());
        }
        notificacionRepository.save(notificacion);
    }

    private Notificacion nuevaNotificacion(ConflictoDetectadoEvent evento) {
        return Notificacion.builder()
                .conflicto(conflictoRepository.getReferenceById(evento.conflictoId()))
                .tipo(NotificacionRequestDTO.TIPO_ALERTA)
                .estado(EstadoNotificacion.PENDIENTE_NOTIFICACION)
                .fechaCreacion(LocalDateTime.now(reloj))
                .intentos(0)
                .build();
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
