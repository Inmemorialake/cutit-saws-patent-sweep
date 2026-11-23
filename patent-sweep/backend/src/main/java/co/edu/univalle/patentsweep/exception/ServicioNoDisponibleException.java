package co.edu.univalle.patentsweep.exception;

/**
 * Una API externa no respondió a tiempo o devolvió un error del servidor. Es la única excepción
 * que Resilience4j reintenta; al agotarse los intentos se responde 503 (PB HU-02, criterio 7).
 */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
