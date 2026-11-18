package co.edu.univalle.patentsweep.controller.dto;

public record PatenteResponseDTO(
        Long patenteId,
        String codigoPatente,
        String titulo,
        String tipoProducto,
        String resumen,
        String estadoVigencia
) { }
