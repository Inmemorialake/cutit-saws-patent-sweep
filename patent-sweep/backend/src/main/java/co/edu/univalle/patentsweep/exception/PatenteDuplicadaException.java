package co.edu.univalle.patentsweep.exception;

public class PatenteDuplicadaException extends RuntimeException {
    public PatenteDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
