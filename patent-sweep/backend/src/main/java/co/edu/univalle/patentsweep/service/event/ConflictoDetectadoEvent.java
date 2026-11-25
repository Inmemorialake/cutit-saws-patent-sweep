package co.edu.univalle.patentsweep.service.event;

/**
 * Evento de dominio (P12): se publica al registrar un conflicto nuevo en estado DETECTADO.
 * Quien lo procesa (NotificacionService) lo hace aparte y sin bloquear al emisor (PB HU-12).
 */
public record ConflictoDetectadoEvent(Long conflictoId, String formulaId, String patenteId) {
}
