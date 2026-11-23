package co.edu.univalle.patentsweep.exception;

/** La fórmula está DESCONTINUADA y no admite búsqueda (PB HU-02, criterio 6.3: HTTP 409). */
public class FormulaDescontinuadaException extends RuntimeException {

    public FormulaDescontinuadaException() {
        super("No se puede buscar patentes para una fórmula DESCONTINUADA");
    }
}
