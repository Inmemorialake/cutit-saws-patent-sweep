package co.edu.univalle.patentsweep.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** JWT emitido para una sesión autenticada. expiresIn se expresa en segundos. */
public record LoginResponseDTO(
        @Schema(description = "Token JWT para enviar como Bearer", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Esquema de autenticación", example = "Bearer")
        String tokenType,
        @Schema(description = "Segundos hasta que expire el token", example = "3600")
        long expiresIn,
        @Schema(description = "Usuario autenticado", example = "admin")
        String username,
        @Schema(description = "Rol asignado al usuario", example = "ADMINISTRADOR")
        String role) {
}
