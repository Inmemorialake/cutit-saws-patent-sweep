package co.edu.univalle.patentsweep.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("La solicitud no es válida");
        return responder(HttpStatus.BAD_REQUEST, "Solicitud inválida", detalle);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> manejarConstraintViolation(ConstraintViolationException ex) {
        String detalle = ex.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("Parámetro de consulta no válido");
        return responder(HttpStatus.BAD_REQUEST, "Solicitud inválida", detalle);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "El cuerpo de la petición no es válido");
    }

    @ExceptionHandler(PatenteNoEncontradaException.class)
    public ResponseEntity<ProblemDetail> manejarPatenteNoEncontrada(PatenteNoEncontradaException ex) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(PatenteDuplicadaException.class)
    public ResponseEntity<ProblemDetail> manejarPatenteDuplicada(PatenteDuplicadaException ex) {
        return responder(HttpStatus.CONFLICT, "Conflicto de negocio", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarErrorInterno(Exception ex) {
        log.error("Error interno no controlado", ex);
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
