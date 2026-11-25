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

    @Test
    @DisplayName("404 cuando la fórmula consultada no existe, con su traceId")
    void deberiaConvertirFormulaNoEncontradaEnProblemDetail() {
        ResponseEntity<ProblemDetail> respuesta = manejador.manejarFormulaNoEncontrada(new FormulaNoEncontradaException());

        verificar(respuesta, HttpStatus.NOT_FOUND, "La fórmula consultada no fue encontrada");
        assertThat(respuesta.getBody().getProperties()).containsEntry("traceId", "traza-123");
    }

    @Test
    @DisplayName("409 cuando la fórmula está descontinuada")
    void deberiaConvertirFormulaDescontinuadaEnProblemDetail() {
        verificar(manejador.manejarFormulaDescontinuada(new FormulaDescontinuadaException()),
                HttpStatus.CONFLICT, "No se puede buscar patentes para una fórmula DESCONTINUADA");
    }

    @Test
    @DisplayName("503 cuando un servicio externo no está disponible")
    void deberiaConvertirServicioNoDisponibleEnProblemDetail() {
        verificar(manejador.manejarServicioNoDisponible(
                        new ServicioNoDisponibleException("timeout", new IllegalStateException("causa"))),
                HttpStatus.SERVICE_UNAVAILABLE, "No fue posible realizar la consulta. Intente nuevamente");
    }

    @Test
    @DisplayName("401 cuando las credenciales son inválidas")
    void deberiaConvertirCredencialesInvalidasEnProblemDetail() {
        verificar(manejador.manejarCredenciales(new BadCredentialsException("clave incorrecta")),
                HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }

    @Test
    @DisplayName("403 cuando el usuario no tiene permisos")
    void deberiaConvertirAccesoDenegadoEnProblemDetail() {
        verificar(manejador.manejarAccesoDenegado(new AccessDeniedException("sin rol")),
                HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación");
    }

    @Test
    @DisplayName("500 sin filtrar detalles de una integración externa")
    void deberiaOcultarDetallesDeIntegracionExterna() {
        verificar(manejador.manejarErrorInterno(
                        new IntegracionExternaException("token interno", new IllegalStateException("causa"))),
                HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible procesar la solicitud. Intente nuevamente");
    }

    @Test
    @DisplayName("500 sin filtrar detalles de una configuración ausente")
    void deberiaOcultarDetallesDeConfiguracionAusente() {
        verificar(manejador.manejarErrorInterno(new ConfiguracionNoEncontradaException()),
                HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible procesar la solicitud. Intente nuevamente");
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
