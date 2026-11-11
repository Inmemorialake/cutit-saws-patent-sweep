package co.edu.univalle.patentsweep.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitud de evaluación de una fórmula a Materiales/Fórmulas (ER: EvaluacionFormula). Por ahora solo se
 * consulta, para calcular PENDIENTE_REVISION (HU-04); HU-03 la va a escribir.
 */
@Entity
@Table(name = "evaluacion_formula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionFormula {

    @Id
    @Column(name = "evaluacion_id")
    private UUID evaluacionId;

    @Column(name = "formula_id", nullable = false, length = 20)
    private String formulaId;

    @Column(nullable = false, length = 100)
    private String ticket;

    @Column(nullable = false, length = 30)
    private String estado;
}
