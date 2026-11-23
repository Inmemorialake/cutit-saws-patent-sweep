package co.edu.univalle.patentsweep.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Credenciales de POST /api/v1/auth/login. */
public record LoginRequestDTO(
        @Schema(description = "Nombre de usuario", example = "admin")
        @NotBlank(message = "El usuario es obligatorio")
        String usuario,
        @Schema(description = "Clave del usuario")
        @NotBlank(message = "La clave es obligatoria")
        String clave) {
}
