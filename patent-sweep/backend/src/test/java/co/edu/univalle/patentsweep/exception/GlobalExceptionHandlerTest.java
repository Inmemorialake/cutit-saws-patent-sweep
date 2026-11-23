package co.edu.univalle.patentsweep.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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
    @DisplayName("404 con el mensaje del PB y el traceId")
    void deberiaConvertirFormulaNoEncontradaEnProblemDetail() {
        ResponseEntity<ProblemDetail> respuesta = manejador.manejarFormulaNoEncontrada(new FormulaNoEncontradaException());

        verificar(respuesta, HttpStatus.NOT_FOUND, "La fórmula consultada no fue encontrada");
        assertThat(respuesta.getBody().getProperties()).containsEntry("traceId", "traza-123");
    }

    @Test
    @DisplayName("409 con el mensaje del PB")
    void deberiaConvertirFormulaDescontinuadaEnProblemDetail() {
        verificar(manejador.manejarFormulaDescontinuada(new FormulaDescontinuadaException()), HttpStatus.CONFLICT,
                "No se puede buscar patentes para una fórmula DESCONTINUADA");
    }

    @Test
    @DisplayName("503 ante un servicio no disponible o un Circuit Breaker abierto")
    void deberiaConvertirServicioNoDisponibleEnProblemDetail() {
        String mensaje = "No fue posible realizar la consulta. Intente nuevamente";

        verificar(manejador.manejarServicioNoDisponible(new ServicioNoDisponibleException("timeout")),
                HttpStatus.SERVICE_UNAVAILABLE, mensaje);
        CallNotPermittedException abierto = CallNotPermittedException
                .createCallNotPermittedException(CircuitBreaker.ofDefaults("prueba"));
        verificar(manejador.manejarServicioNoDisponible(abierto), HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }

    @Test
    @DisplayName("401 ante credenciales inválidas y 403 sin permisos")
    void deberiaConvertirErroresDeSeguridad() {
        verificar(manejador.manejarCredenciales(new BadCredentialsException("x")), HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas");
        verificar(manejador.manejarAccesoDenegado(new AccessDeniedException("x")), HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta operación");
    }

    @Test
    @DisplayName("500 genérico sin detalles internos")
    void deberiaOcultarLosDetallesDeUnErrorInterno() {
        verificar(manejador.manejarErrorInterno(new IllegalStateException("clave secreta")),
                HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible procesar la solicitud. Intente nuevamente");
    }

    @Test
    @DisplayName("400 por cuerpo ilegible")
    void deberiaConvertirUnCuerpoIlegible() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("x", new MockHttpInputMessage(new byte[0]));

        verificar(manejador.manejarCuerpoIlegible(ex), HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");
    }

    @Test
    @DisplayName("400 con el mensaje del primer campo inválido, o uno genérico si no hay campos")
    void deberiaConvertirErroresDeValidacion() throws Exception {
        // Arrange
        MethodParameter parametro = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDeEjemplo", String.class), 0);
        BeanPropertyBindingResult conError = new BeanPropertyBindingResult(new Object(), "solicitud");
        conError.addError(new FieldError("solicitud", "formulaId", "Mensaje del campo"));
        BeanPropertyBindingResult sinErrores = new BeanPropertyBindingResult(new Object(), "solicitud");

        // Act / Assert
        verificar(manejador.manejarValidacion(new MethodArgumentNotValidException(parametro, conError)),
                HttpStatus.BAD_REQUEST, "Mensaje del campo");
        verificar(manejador.manejarValidacion(new MethodArgumentNotValidException(parametro, sinErrores)),
                HttpStatus.BAD_REQUEST, "La solicitud no es válida");
    }

    @SuppressWarnings("unused")
    private void metodoDeEjemplo(String valor) {
    }

    private static void verificar(ResponseEntity<ProblemDetail> respuesta, HttpStatus estado, String detalle) {
        assertThat(respuesta.getStatusCode()).isEqualTo(estado);
        assertThat(respuesta.getBody().getDetail()).isEqualTo(detalle);
        assertThat(respuesta.getBody().getTitle()).isNotBlank();
    }
}
