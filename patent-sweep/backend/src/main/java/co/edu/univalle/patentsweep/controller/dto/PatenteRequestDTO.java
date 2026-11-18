package co.edu.univalle.patentsweep.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatenteRequestDTO(
        @NotBlank(message = "El código de la patente es obligatorio")
        @Pattern(regexp = "^PAT-\\d{6}$", message = "El código debe tener el formato PAT-NNNNNN")
        String codigoPatente,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 255, message = "El título no puede superar 255 caracteres")
        String titulo,

        @Size(max = 60, message = "El tipo de producto no puede superar 60 caracteres")
        String tipoProducto, // Opcional

        String resumen // Opcional
) { }
