package co.edu.univalle.patentsweep.exception;

/** La fórmula no existe en Materiales/Fórmulas (PB HU-02, criterio 6.2: HTTP 404). */
public class FormulaNoEncontradaException extends RuntimeException {

    public FormulaNoEncontradaException() {
        super("La fórmula consultada no fue encontrada");
    }
}
