package co.edu.univalle.patentsweep.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frecuencia y criterios de búsqueda; solo una fila activa a la vez (ER: Configuracion). */
@Entity
@Table(name = "configuracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Configuracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "configuracion_id")
    private Long configuracionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Positive(message = "La frecuencia debe ser mayor que cero")
    @Column(name = "frecuencia_valor", nullable = false)
    private int frecuenciaValor;

    /** HORAS, DIAS o SEMANAS. */
    @Pattern(regexp = "HORAS|DIAS|SEMANAS", message = "La unidad de frecuencia debe ser HORAS, DIAS o SEMANAS")
    @Column(name = "frecuencia_unidad", nullable = false, length = 10)
    private String frecuenciaUnidad;

    @Positive(message = "La frecuencia en horas debe ser mayor que cero")
    @Column(name = "frecuencia_horas", nullable = false)
    private int frecuenciaHoras;

    @Positive(message = "El rango de fechas debe ser mayor que cero")
    @Column(name = "rango_cantidad")
    private Integer rangoCantidad;

    /** MESES o ANOS; nulo cuando no se limita por fecha. */
    @Pattern(regexp = "MESES|ANOS", message = "La unidad del rango debe ser MESES o ANOS")
    @Column(name = "rango_unidad", length = 10)
    private String rangoUnidad;

    @NotNull(message = "Las clasificaciones son obligatorias")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> clasificaciones;

    @Column(nullable = false)
    private boolean activa;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDateTime vigenteDesde;

    @Column(name = "trace_id", length = 100)
    private String traceId;
}
