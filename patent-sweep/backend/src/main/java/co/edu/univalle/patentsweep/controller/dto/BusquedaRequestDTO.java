package co.edu.univalle.patentsweep.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de POST /api/v1/patentes/busqueda */
public record BusquedaRequestDTO(
        @Schema(description = "Código de la fórmula en Materiales/Fórmulas", example = "form-001")
        @NotBlank(message = BusquedaRequestDTO.MENSAJE_FORMULA)
        @Pattern(regexp = "^form-\\d{3}$", message = BusquedaRequestDTO.MENSAJE_FORMULA)
        String formulaId) {

    public static final String MENSAJE_FORMULA =
            "El código de la fórmula es obligatorio y debe tener el formato form-NNN";
}
