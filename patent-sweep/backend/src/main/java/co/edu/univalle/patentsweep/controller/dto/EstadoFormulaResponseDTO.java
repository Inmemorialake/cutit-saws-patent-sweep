package co.edu.univalle.patentsweep.controller.dto;

import co.edu.univalle.patentsweep.model.enums.EstadoFormula;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Respuesta del estado calculado de una fórmula para Fabricación (PB HU-04). */
public record EstadoFormulaResponseDTO(
        @Schema(description = "Código de la fórmula consultada", example = "form-001")
        String formulaId,
        @Schema(description = "Estado actual de la fórmula frente a patentes", example = "LIBRE")
        EstadoFormula estado,
        @Schema(description = "Fecha y hora de la última revisión efectuada", example = "2026-10-19T10:30:00")
        LocalDateTime fechaUltimaRevision,
        @Schema(description = "Mensaje descriptivo para el proceso de fabricación")
        String mensaje) {
}
