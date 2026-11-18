package co.edu.univalle.patentsweep.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Restricciones de la entidad Patente: los datos inválidos se rechazan antes de llegar a la base. */
class RestriccionesEntidadesTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    @Test
    @DisplayName("Una patente con código PAT-NNNNNN y título es válida")
    void deberiaAceptarUnaPatenteValida() {
        assertThat(validador.validate(patenteValida())).isEmpty();
    }

    @Test
    @DisplayName("Una patente con código mal formado o título vacío se rechaza")
    void deberiaRechazarUnaPatenteInvalida() {
        // Arrange
        Patente patente = patenteValida();
        patente.setCodigoPatente("PAT-2026-999");
        patente.setTitulo("  ");

        // Act / Assert
        assertThat(propiedades(validador.validate(patente))).containsExactlyInAnyOrder("codigoPatente", "titulo");
    }

    private static Patente patenteValida() {
        return Patente.builder().codigoPatente("PAT-100001").titulo("Aleación para cadenas de corte").build();
    }

    private static <T> Set<String> propiedades(Set<ConstraintViolation<T>> violaciones) {
        return violaciones.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}
