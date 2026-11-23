package co.edu.univalle.patentsweep.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Token de sesión emitido por el login. */
public record LoginResponseDTO(
        @Schema(description = "JWT firmado; se envía en el encabezado Authorization: Bearer <token>")
        String token,
        @Schema(description = "Tipo de token", example = "Bearer")
        String tipo,
        @Schema(description = "Minutos de vigencia del token", example = "60")
        long expiraEnMinutos,
        @Schema(description = "Rol del usuario", example = "ADMINISTRADOR")
        String rol) {
}
