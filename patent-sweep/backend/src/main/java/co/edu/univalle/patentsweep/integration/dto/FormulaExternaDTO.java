package co.edu.univalle.patentsweep.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Versión que se guarda en conflictos y se compara al consultar el estado (DA-17). */
    public static final String SIN_VERSION = "SIN_VERSION";

    public String versionRegistrada() {
        return version == null ? SIN_VERSION : version;
    }

    /**
     * Foto de la fórmula tal como se evaluó (código, versión, aplicación y composición con porcentajes). Se
     * guarda en la revisión para poder demostrar con qué datos se hizo, aunque Materiales los cambie después.
     */
    public Map<String, Object> foto() {
        List<Map<String, Object>> composicion = new ArrayList<>();
        if (componentes != null) {
            for (ComponenteExternoDTO componente : componentes) {
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("skuMaterial", componente.skuMaterial());
                fila.put("nombreMaterial", componente.nombreMaterial());
                fila.put("porcentaje", componente.porcentaje());
                composicion.add(fila);
            }
        }
        Map<String, Object> foto = new LinkedHashMap<>();
        foto.put("codigo", codigo);
        foto.put("version", versionRegistrada());
        foto.put("nombre", nombre);
        foto.put("aplicacionTecnica", aplicacionTecnica);
        foto.put("estado", estado);
        foto.put("componentes", composicion);
        return foto;
    }

    public boolean estaDescontinuada() {
        return ESTADO_DESCONTINUADA.equals(estado);
    }
}
