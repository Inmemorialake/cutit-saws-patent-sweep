package co.edu.univalle.patentsweep.exception;

/** No existe una configuración activa; la migración inicial debería haberla precargado. */
public class ConfiguracionNoEncontradaException extends RuntimeException {

    public ConfiguracionNoEncontradaException() {
        super("No existe una configuración de criterios de búsqueda activa");
    }
}
