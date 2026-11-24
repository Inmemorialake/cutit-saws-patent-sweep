package co.edu.univalle.patentsweep.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.patentsweep.integration.dto.ComponenteExternoDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MotorCoincidenciasTest {

    private final MotorCoincidencias motor = new MotorCoincidencias(50, 2);

    @Test
    @DisplayName("Una patente que menciona 3 de 4 materiales es un conflicto, sin importar tildes ni mayúsculas")
    void deberiaDetectarConflictoCuandoMencionaMasDeLaMitadDeLosMateriales() {
        // Arrange
        FormulaExternaDTO formula = formula("Cadena de corte",
                "Hierro", "Cobalto", "Níquel", "Carbono");
        PatenteExternaDTO patente = patente("PAT-000001", "ALEACIÓN", "Aleación de hierro con cobalto y NIQUEL.",
                List.of("1. Una aleación metálica."));

        // Act
        List<PatenteExternaDTO> resultado = motor.filtrarConflictos(formula, List.of(patente));

        // Assert
        assertThat(resultado).containsExactly(patente);
    }

    @Test
    @DisplayName("Una patente que menciona solo 1 de 3 materiales no es un conflicto")
    void noDeberiaDetectarConflictoCuandoMencionaMenosDelMinimoRequerido() {
        // Arrange
        FormulaExternaDTO formula = formula("Disco", "Titanio", "Aluminio", "Magnesio");
        PatenteExternaDTO patente = patente("PAT-000002", "Recubrimiento de titanio", "Capa delgada.",
                List.of("1. Un recubrimiento."));

        // Act
        List<PatenteExternaDTO> resultado = motor.filtrarConflictos(formula, List.of(patente));

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("El mínimo de 2 materiales prevalece sobre el 50 % en fórmulas pequeñas")
    void deberiaExigirAlMenosDosMaterialesAunqueElPorcentajeSeaMenor() {
        // Arrange
        FormulaExternaDTO formula = formula("Aleación", "Hierro", "Cobalto");
        PatenteExternaDTO conUno = patente("PAT-000003", "Hierro", "Solo hierro.", List.of());
        PatenteExternaDTO conDos = patente("PAT-000004", "Hierro", "Hierro y cobalto.", List.of());

        // Act
        List<PatenteExternaDTO> resultado = motor.filtrarConflictos(formula, List.of(conUno, conDos));

        // Assert
        assertThat(resultado).containsExactly(conDos);
    }

    @Test
    @DisplayName("Una fórmula con un solo material nunca produce conflictos")
    void noDeberiaDetectarConflictoCuandoLaFormulaTieneMenosDelMinimoDeMateriales() {
        // Arrange
        FormulaExternaDTO formula = formula("Aleación", "Hierro");
        PatenteExternaDTO patente = patente("PAT-000005", "Hierro", "Hierro puro.", List.of());

        // Act
        List<PatenteExternaDTO> resultado = motor.filtrarConflictos(formula, List.of(patente));

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Una fórmula sin componentes no produce conflictos")
    void noDeberiaDetectarConflictoCuandoLaFormulaNoTieneComponentes() {
        // Arrange
        FormulaExternaDTO formula = new FormulaExternaDTO("form-009", "Vacía", "Sin materiales", "1",
                "APROBADA", null);
        PatenteExternaDTO patente = patente("PAT-000006", "Cualquiera", "Texto.", List.of());

        // Act
        List<PatenteExternaDTO> resultado = motor.filtrarConflictos(formula, List.of(patente));

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Los nombres de material vacíos o repetidos se ignoran y los campos nulos de la patente se toleran")
    void deberiaIgnorarNombresVaciosRepetidosYTolerarCamposNulos() {
        // Arrange
        FormulaExternaDTO formula = new FormulaExternaDTO("form-010", "Mezcla", "Uso", "1", "APROBADA",
                Arrays.asList(
                        new ComponenteExternoDTO("S1", "Hierro", BigDecimal.ONE),
                        new ComponenteExternoDTO("S2", " hierro ", BigDecimal.ONE),
                        new ComponenteExternoDTO("S3", "  ", BigDecimal.ONE),
                        new ComponenteExternoDTO("S4", null, BigDecimal.ONE),
                        new ComponenteExternoDTO("S5", "Cobalto", BigDecimal.ONE)));
        PatenteExternaDTO conTextoNulo = new PatenteExternaDTO("PAT-000007", null, null,
                EstadoVigenciaPatente.CONCEDIDA, null, null, null, null, null, null, null);
        PatenteExternaDTO coincidente = patente("PAT-000008", "Hierro", "Con cobalto.", List.of());

        // Act
        List<PatenteExternaDTO> resultado = motor.filtrarConflictos(formula, List.of(conTextoNulo, coincidente));

        // Assert
        assertThat(resultado).containsExactly(coincidente);
    }

    @Test
    @DisplayName("Los términos de búsqueda incluyen los materiales y la aplicación técnica, sin repetir")
    void deberiaArmarTerminosConMaterialesYAplicacionTecnica() {
        // Arrange
        FormulaExternaDTO formula = formula("Cadena de corte", "Hierro", "Hierro", "Cobalto");

        // Act
        List<String> terminos = motor.terminosDeBusqueda(formula);

        // Assert
        assertThat(terminos).containsExactly("Hierro", "Cobalto", "Cadena de corte");
    }

    @Test
    @DisplayName("La aplicación técnica nula o vacía no se agrega a los términos")
    void noDeberiaAgregarAplicacionTecnicaNulaOVacia() {
        // Arrange
        FormulaExternaDTO sinAplicacion = new FormulaExternaDTO("form-011", "A", null, "1", "APROBADA",
                List.of(new ComponenteExternoDTO("S1", "Hierro", BigDecimal.ONE)));
        FormulaExternaDTO aplicacionVacia = new FormulaExternaDTO("form-012", "B", "  ", "1", "APROBADA",
                List.of(new ComponenteExternoDTO("S1", "Hierro", BigDecimal.ONE)));

        // Act
        List<String> terminosA = motor.terminosDeBusqueda(sinAplicacion);
        List<String> terminosB = motor.terminosDeBusqueda(aplicacionVacia);

        // Assert
        assertThat(terminosA).containsExactly("Hierro");
        assertThat(terminosB).containsExactly("Hierro");
    }

    private static FormulaExternaDTO formula(String aplicacion, String... materiales) {
        List<ComponenteExternoDTO> componentes = Arrays.stream(materiales)
                .map(nombre -> new ComponenteExternoDTO("SKU-" + nombre, nombre, BigDecimal.TEN))
                .toList();
        return new FormulaExternaDTO("form-001", "Fórmula de prueba", aplicacion, "1", "APROBADA", componentes);
    }

    private static PatenteExternaDTO patente(String id, String titulo, String resumen, List<String> reivindicaciones) {
        return new PatenteExternaDTO(id, titulo, "Aleación", EstadoVigenciaPatente.CONCEDIDA, null, null, null,
                "US", resumen, reivindicaciones, List.of("B27B 17/00"));
    }
}
