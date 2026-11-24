package co.edu.univalle.patentsweep.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Respuesta 200 de la búsqueda manual: lista de conflictos, vacía si no hubo coincidencias. */
public record BusquedaResponseDTO(
        @Schema(description = "Código de la fórmula consultada", example = "form-001")
        String formulaId,
        @Schema(description = "Conflictos de las patentes coincidentes")
        List<ConflictoResumenDTO> conflictos,
        @Schema(description = "Mensaje descriptivo del resultado")
        String mensaje) {
}
