package co.edu.univalle.patentsweep.integration.dto;

/** Cuerpo de POST /api/v1/notificaciones hacia la API de Notificaciones (PB HU-12, criterio 2). */
public record NotificacionRequestDTO(
        String conflictoId,
        String formulaId,
        String patenteId,
        String tipoAlerta,
        String mensaje) {

    /** Valor fijo del campo tipoAlerta (PB HU-12, criterio 2.4). */
    public static final String TIPO_ALERTA = "CONFLICTO_PATENTE_DETECTADO";
}
