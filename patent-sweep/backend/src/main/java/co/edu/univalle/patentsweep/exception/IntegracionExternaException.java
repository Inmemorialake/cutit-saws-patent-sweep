package co.edu.univalle.patentsweep.exception;

/**
 * Una API externa rechazó la petición (error 4xx distinto de 404) o devolvió un contenido
 * ilegible. No se reintenta; para el cliente es un fallo interno (HTTP 500).
 */
public class IntegracionExternaException extends RuntimeException {

    public IntegracionExternaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
