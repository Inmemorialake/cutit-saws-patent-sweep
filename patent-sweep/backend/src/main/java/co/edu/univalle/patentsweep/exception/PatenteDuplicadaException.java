package co.edu.univalle.patentsweep.exception;

/** Código de patente repetido; el handler la responde como 409. */
public class PatenteDuplicadaException extends RuntimeException {
    public PatenteDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
