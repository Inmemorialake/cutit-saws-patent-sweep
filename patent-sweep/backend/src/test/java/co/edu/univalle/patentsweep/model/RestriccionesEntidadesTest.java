package co.edu.univalle.patentsweep.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Restricciones de las entidades: los datos inválidos se rechazan antes de llegar a la base. */
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

    @Test
    @DisplayName("Un conflicto con fórmula mal formada o sin versión se rechaza")
    void deberiaRechazarUnConflictoInvalido() {
        // Arrange
        Conflicto conflicto = Conflicto.builder().formulaId("FORM-1").versionFormula(" ").build();

        // Act / Assert
        assertThat(propiedades(validador.validate(conflicto))).containsExactlyInAnyOrder("formulaId", "versionFormula");
    }

    @Test
    @DisplayName("Un conflicto con fórmula form-NNN y versión es válido")
    void deberiaAceptarUnConflictoValido() {
        Conflicto conflicto = Conflicto.builder().formulaId("form-001").versionFormula("2").build();

        assertThat(validador.validate(conflicto)).isEmpty();
    }

    @Test
    @DisplayName("Una revisión con fórmula mal formada o versión demasiado larga se rechaza")
    void deberiaRechazarUnaRevisionInvalida() {
        // Arrange
        RevisionFormula revision = RevisionFormula.builder().formulaId("form-1")
                .versionFormula("x".repeat(21)).build();

        // Act / Assert
        assertThat(propiedades(validador.validate(revision))).containsExactlyInAnyOrder("formulaId", "versionFormula");
    }

    @Test
    @DisplayName("Una evaluación sin ticket o con fórmula inválida se rechaza")
    void deberiaRechazarUnaEvaluacionInvalida() {
        // Arrange
        EvaluacionFormula evaluacion = EvaluacionFormula.builder().formulaId("001").ticket("").build();

        // Act / Assert
        assertThat(propiedades(validador.validate(evaluacion))).containsExactlyInAnyOrder("formulaId", "ticket");
    }

    @Test
    @DisplayName("Un usuario sin nombre ni clave se rechaza")
    void deberiaRechazarUnUsuarioInvalido() {
        // Arrange
        Usuario usuario = Usuario.builder().username("").passwordHash("").build();

        // Act / Assert
        assertThat(propiedades(validador.validate(usuario))).containsExactlyInAnyOrder("username", "passwordHash");
    }

    @Test
    @DisplayName("Una configuración con frecuencia cero y unidades inválidas se rechaza")
    void deberiaRechazarUnaConfiguracionInvalida() {
        // Arrange
        Configuracion configuracion = Configuracion.builder()
                .frecuenciaValor(0).frecuenciaUnidad("MINUTOS").frecuenciaHoras(0)
                .rangoCantidad(-1).rangoUnidad("DIAS").clasificaciones(null).build();

        // Act / Assert
        assertThat(propiedades(validador.validate(configuracion))).containsExactlyInAnyOrder("frecuenciaValor",
                "frecuenciaUnidad", "frecuenciaHoras", "rangoCantidad", "rangoUnidad", "clasificaciones");
    }

    @Test
    @DisplayName("Una configuración sin rango de fechas es válida")
    void deberiaAceptarUnaConfiguracionSinRango() {
        Configuracion configuracion = Configuracion.builder()
                .frecuenciaValor(1).frecuenciaUnidad("DIAS").frecuenciaHoras(24)
                .clasificaciones(List.of("B27B 17/00")).build();

        assertThat(validador.validate(configuracion)).isEmpty();
    }

    @Test
    @DisplayName("Una notificación sin tipo o con intentos negativos se rechaza")
    void deberiaRechazarUnaNotificacionInvalida() {
        // Arrange
        Notificacion notificacion = Notificacion.builder().tipo(" ").intentos(-1).build();

        // Act / Assert
        assertThat(propiedades(validador.validate(notificacion))).containsExactlyInAnyOrder("tipo", "intentos");
    }

    private static Patente patenteValida() {
        return Patente.builder().codigoPatente("PAT-100001").titulo("Aleación para cadenas de corte").build();
    }

    private static <T> Set<String> propiedades(Set<ConstraintViolation<T>> violaciones) {
        return violaciones.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}
