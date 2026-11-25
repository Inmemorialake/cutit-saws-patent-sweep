package co.edu.univalle.patentsweep.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** Componente de una fórmula tal como lo expone Materiales/Fórmulas (PB HU-01, criterio 3.2). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ComponenteExternoDTO(String skuMaterial, String nombreMaterial, BigDecimal porcentaje) {
}
