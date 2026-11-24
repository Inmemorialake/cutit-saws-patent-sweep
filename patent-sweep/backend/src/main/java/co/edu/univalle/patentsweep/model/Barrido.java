package co.edu.univalle.patentsweep.model;

import co.edu.univalle.patentsweep.model.enums.EstadoBarrido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ejecución de un barrido. D-10: se reutilizan las columnas V1 `total_fallidos` y `mensaje_error`
 * con los nombres de dominio `noProcesadas` y `mensaje`.
 */
@Entity
@Table(name = "barrido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Barrido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "barrido_id")
    private Long barridoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoBarrido estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "total_formulas")
    private Integer totalFormulas;

    @Column(name = "total_procesadas")
    private Integer totalProcesadas;

    @Column(name = "total_con_conflicto")
    private Integer totalConConflicto;

    @Column(name = "total_sin_conflicto")
    private Integer totalSinConflicto;

    @Column(name = "total_fallidos")
    private Integer noProcesadas;

    @Column(name = "mensaje_error")
    private String mensaje;
}
