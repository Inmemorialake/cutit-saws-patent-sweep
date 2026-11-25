package co.edu.univalle.patentsweep.model.enums;

/** Estados del barrido periódico (HU-01, P17). */
public enum EstadoBarrido {
    EN_CURSO,
    COMPLETADO,
    FALLIDO,
    /** HU-01 criterio 1.1: no se inició porque ya había un barrido en curso; queda solo como auditoría. */
    OMITIDO
}
