package co.edu.univalle.patentsweep.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Detalle de una fórmula devuelto por GET /formulas/{codigo} de Materiales/Fórmulas. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FormulaExternaDTO(
        String codigo,
        String nombre,
        String aplicacionTecnica,
        String version,
        String estado,
        List<ComponenteExternoDTO> componentes) {

    /** Estado con el que Materiales/Fórmulas marca una fórmula retirada. */
    public static final String ESTADO_DESCONTINUADA = "DESCONTINUADA";

    public boolean estaDescontinuada() {
        return ESTADO_DESCONTINUADA.equals(estado);
    }
}
