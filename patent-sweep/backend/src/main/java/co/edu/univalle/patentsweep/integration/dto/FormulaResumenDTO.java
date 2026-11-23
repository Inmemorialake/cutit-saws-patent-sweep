package co.edu.univalle.patentsweep.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/** Resumen de una fórmula aprobada devuelta por `GET /formulas?estado=APROBADA&pagina={n}` (HU-01). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FormulaResumenDTO(
        String codigo,
        String nombre,
        String aplicacionTecnica,
        String estado,
        LocalDateTime fechaUltimaActualizacion) {
}
