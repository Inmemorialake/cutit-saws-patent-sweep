package co.edu.univalle.patentsweep.exception;

public class PatenteNoEncontradaException extends RuntimeException {
    public PatenteNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
