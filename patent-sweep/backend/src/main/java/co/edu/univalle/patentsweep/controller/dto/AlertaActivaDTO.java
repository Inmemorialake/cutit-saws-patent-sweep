package co.edu.univalle.patentsweep.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Conflicto vigente de una fórmula, mostrado a Fabricación como alerta activa (PB HU-04). */
public record AlertaActivaDTO(
        @Schema(description = "Identificador del conflicto (6 dígitos)", example = "100001")
        String conflictoId,
        @Schema(description = "Título de la patente en conflicto (máximo 60 caracteres)",
                example = "Aleación para cadenas de corte")
        String tituloPatente,
        @Schema(description = "Fecha y hora en que se detectó el conflicto", example = "2026-10-19T10:30:00")
        LocalDateTime fechaDeteccion) {
}
