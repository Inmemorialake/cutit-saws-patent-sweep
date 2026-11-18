package co.edu.univalle.patentsweep.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();

    @BeforeEach
    void ponerTraceId() {
        MDC.put("traceId", "traza-123");
    }

    @AfterEach
    void limpiarTraceId() {
        MDC.clear();
    }

    @Test
    @DisplayName("404 cuando no se encuentra la patente, con su traceId")
    void deberiaConvertirPatenteNoEncontradaEnProblemDetail() {
        ResponseEntity<ProblemDetail> respuesta = manejador.manejarPatenteNoEncontrada(new PatenteNoEncontradaException("No encontrada"));

        verificar(respuesta, HttpStatus.NOT_FOUND, "No encontrada");
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getProperties()).containsEntry("traceId", "traza-123");
    }

    @Test
    @DisplayName("409 cuando la patente ya existe (duplicada)")
    void deberiaConvertirPatenteDuplicadaEnProblemDetail() {
        verificar(manejador.manejarPatenteDuplicada(new PatenteDuplicadaException("La patente ya existe")),
                HttpStatus.CONFLICT, "La patente ya existe");
    }

    @Test
    @DisplayName("500 genérico ocultando los detalles internos del error")
    void deberiaOcultarLosDetallesDeUnErrorInterno() {
        verificar(manejador.manejarErrorInterno(new IllegalStateException("clave secreta")),
                HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible procesar la solicitud. Intente nuevamente");
    }

    @Test
    @DisplayName("400 por cuerpo de petición ilegible o malformado")
    void deberiaConvertirUnCuerpoIlegible() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("x", new MockHttpInputMessage(new byte[0]));

        verificar(manejador.manejarCuerpoIlegible(ex), HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");
    }

    @Test
    @DisplayName("400 con el mensaje del primer campo inválido en validación del DTO")
    void deberiaConvertirErroresDeValidacion() throws Exception {
        MethodParameter parametro = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDeEjemplo", String.class), 0);
        BeanPropertyBindingResult conError = new BeanPropertyBindingResult(new Object(), "solicitud");
        conError.addError(new FieldError("solicitud", "codigoPatente", "El código es obligatorio"));
        BeanPropertyBindingResult sinErrores = new BeanPropertyBindingResult(new Object(), "solicitud");

        verificar(manejador.manejarValidacion(new MethodArgumentNotValidException(parametro, conError)),
                HttpStatus.BAD_REQUEST, "El código es obligatorio");
        verificar(manejador.manejarValidacion(new MethodArgumentNotValidException(parametro, sinErrores)),
                HttpStatus.BAD_REQUEST, "La solicitud no es válida");
    }

    @Test
    @DisplayName("400 ante violación de restricciones de parámetros")
    void deberiaConvertirConstraintViolation() {
        ConstraintViolationException ex = new ConstraintViolationException("Parámetro inválido", Set.of());
        verificar(manejador.manejarConstraintViolation(ex), HttpStatus.BAD_REQUEST, "Parámetro de consulta no válido");
    }

    @SuppressWarnings("unused")
    private void metodoDeEjemplo(String valor) {
    }

    private static void verificar(ResponseEntity<ProblemDetail> respuesta, HttpStatus estado, String detalle) {
        assertThat(respuesta.getStatusCode()).isEqualTo(estado);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getDetail()).isEqualTo(detalle);
        assertThat(respuesta.getBody().getTitle()).isNotBlank();
    }
}
