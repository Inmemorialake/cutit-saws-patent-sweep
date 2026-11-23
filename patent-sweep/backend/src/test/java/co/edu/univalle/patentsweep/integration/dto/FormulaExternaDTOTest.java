package co.edu.univalle.patentsweep.integration.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FormulaExternaDTOTest {

    @Test
    @DisplayName("La foto incluye código, versión, aplicación y la composición con porcentajes")
    void deberiaArmarLaFotoConLaComposicion() {
        // Arrange
        FormulaExternaDTO formula = new FormulaExternaDTO("form-001", "Aleación A", "Cadena de corte", "2",
                "APROBADA", List.of(new ComponenteExternoDTO("mat-001", "Hierro", new BigDecimal("60.5"))));

        // Act
        Map<String, Object> foto = formula.foto();

        // Assert
        assertThat(foto).containsEntry("codigo", "form-001")
                .containsEntry("version", "2")
                .containsEntry("nombre", "Aleación A")
                .containsEntry("aplicacionTecnica", "Cadena de corte")
                .containsEntry("estado", "APROBADA");
        assertThat(foto.get("componentes")).isEqualTo(List.of(Map.of("skuMaterial", "mat-001",
                "nombreMaterial", "Hierro", "porcentaje", new BigDecimal("60.5"))));
    }

    @Test
    @DisplayName("Sin versión ni componentes la foto usa SIN_VERSION y una composición vacía")
    void deberiaArmarLaFotoSinVersionNiComponentes() {
        // Arrange
        FormulaExternaDTO formula = new FormulaExternaDTO("form-001", null, null, null, null, null);

        // Act
        Map<String, Object> foto = formula.foto();

        // Assert
        assertThat(foto).containsEntry("version", "SIN_VERSION");
        assertThat(foto.get("componentes")).isEqualTo(List.of());
        assertThat(formula.versionRegistrada()).isEqualTo("SIN_VERSION");
    }
}
