package co.edu.univalle.patentsweep.controller.dto;

/** Respuesta de la API: solo los campos que se exponen al cliente. */
public record PatenteResponseDTO(
        Long patenteId,
        String codigoPatente,
        String titulo,
        String tipoProducto,
        String resumen,
        String estadoVigencia
) { }
