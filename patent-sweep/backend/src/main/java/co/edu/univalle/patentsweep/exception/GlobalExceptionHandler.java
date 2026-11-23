package co.edu.univalle.patentsweep.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador global de excepciones (P07): convierte cualquier error en un ProblemDetail
 * (RFC 9457) con el mensaje exacto del PB y el traceId (P19).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger REGISTRO = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("La solicitud no es válida");
        return responder(HttpStatus.BAD_REQUEST, "Solicitud inválida", detalle);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> manejarConstraintViolation(ConstraintViolationException ex) {
        String detalle = ex.getConstraintViolations().stream()
                .findFirst()
                .map(cv -> cv.getMessage())
                .orElse("Parámetro de consulta no válido");
        return responder(HttpStatus.BAD_REQUEST, "Solicitud inválida", detalle);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "El cuerpo de la petición no es válido");
    }

    @ExceptionHandler(FormulaNoEncontradaException.class)
    public ResponseEntity<ProblemDetail> manejarFormulaNoEncontrada(FormulaNoEncontradaException ex) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(FormulaDescontinuadaException.class)
    public ResponseEntity<ProblemDetail> manejarFormulaDescontinuada(FormulaDescontinuadaException ex) {
        return responder(HttpStatus.CONFLICT, "Conflicto de estado", ex.getMessage());
    }

    @ExceptionHandler({ServicioNoDisponibleException.class, CallNotPermittedException.class})
    public ResponseEntity<ProblemDetail> manejarServicioNoDisponible(RuntimeException ex) {
        REGISTRO.error("Servicio externo no disponible: {}", ex.getMessage());
        return responder(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible",
                "No fue posible realizar la consulta. Intente nuevamente");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> manejarCredenciales(AuthenticationException ex) {
        return responder(HttpStatus.UNAUTHORIZED, "No autenticado", "Credenciales inválidas");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> manejarAccesoDenegado(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN, "Acceso denegado",
                "No tiene permisos para realizar esta operación");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarErrorInterno(Exception ex) {
        REGISTRO.error("Error interno no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "No fue posible procesar la solicitud. Intente nuevamente");
    }

    private ResponseEntity<ProblemDetail> responder(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setProperty("traceId", MDC.get("traceId"));
        return ResponseEntity.status(estado).body(problema);
    }
}
