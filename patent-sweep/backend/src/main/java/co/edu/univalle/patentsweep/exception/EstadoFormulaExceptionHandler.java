package co.edu.univalle.patentsweep.exception;

import co.edu.univalle.patentsweep.controller.EstadoFormulaController;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mensajes de error propios de HU-04 (PB 4), distintos a los de HU-02: solo aplica a la consulta de estado.
 * Spring usa el primer advice que tenga un handler compatible, y este captura {@link Exception}, así que
 * los errores que no son suyos (400 y 404) se delegan al {@link GlobalExceptionHandler}.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = EstadoFormulaController.class)
public class EstadoFormulaExceptionHandler {

    static final String MENSAJE_SERVICIO_NO_DISPONIBLE = "No fue posible verificar la fórmula. Intente nuevamente";
    static final String MENSAJE_ERROR_INTERNO = "No fue posible obtener el estado de la fórmula. Intente nuevamente";

    private final GlobalExceptionHandler general;

    public EstadoFormulaExceptionHandler(GlobalExceptionHandler general) {
        this.general = general;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejar(Exception ex) {
        if (ex instanceof ConstraintViolationException violacion) {
            return general.manejarConstraintViolation(violacion);
        }
        if (ex instanceof FormulaNoEncontradaException noEncontrada) {
            return general.manejarFormulaNoEncontrada(noEncontrada);
        }
        if (ex instanceof ServicioNoDisponibleException || ex instanceof CallNotPermittedException) {
            log.error("Materiales no disponible al consultar el estado: {}", ex.getMessage());
            return general.responder(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible",
                    MENSAJE_SERVICIO_NO_DISPONIBLE);
        }
        log.error("Error interno al consultar el estado de la fórmula", ex);
        return general.responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", MENSAJE_ERROR_INTERNO);
    }
}
