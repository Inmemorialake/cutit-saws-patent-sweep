package co.edu.univalle.patentsweep.exception;

/** Patente inexistente; el handler la responde como 404. */
public class PatenteNoEncontradaException extends RuntimeException {
    public PatenteNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
