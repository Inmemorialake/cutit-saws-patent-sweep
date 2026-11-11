package co.edu.univalle.patentsweep.controller.dto;

import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Fila de la tabla de resultados de la búsqueda manual. */
public record ConflictoResumenDTO(
        @JsonProperty("conflictoId")
        @Schema(description = "Identificador del conflicto (6 dígitos)", example = "100001")
        String conflictoId,
        @Schema(description = "Título de la patente (máximo 60 caracteres)", example = "Aleación para cadenas de corte")
        String tituloPatente,
        @Schema(description = "Tipo de producto que protege la patente", example = "Aleación metálica")
        String tipoProducto,
        @Schema(description = "Vigencia de la patente", example = "CONCEDIDA")
        EstadoVigenciaPatente estadoPatente,
        @Schema(description = "Fecha de concesión (dd/mm/aaaa); nula si aún no se concede", example = "22/06/2025")
        LocalDate fechaConcesion) {
}
