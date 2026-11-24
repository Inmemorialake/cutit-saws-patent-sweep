package co.edu.univalle.patentsweep.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Credenciales para POST /api/v1/auth/login. */
public record LoginRequestDTO(
        @Schema(description = "Nombre de usuario existente", example = "admin")
        @NotBlank(message = "El nombre de usuario es obligatorio")
        String username,
        @Schema(description = "Contraseña de la cuenta")
        @NotBlank(message = "La contraseña es obligatoria")
        String password) {
}
