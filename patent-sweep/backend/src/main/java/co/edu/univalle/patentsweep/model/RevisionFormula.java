package co.edu.univalle.patentsweep.model;

import co.edu.univalle.patentsweep.model.enums.OrigenRevision;
import co.edu.univalle.patentsweep.model.enums.ResultadoRevision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Evento del historial de revisiones, solo de inserción (ER: RevisionFormula). */
@Entity
@Table(name = "revision_formula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevisionFormula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revision_id")
    private Long revisionId;

    /** Código de la fórmula en Materiales/Fórmulas (form-NNN); la fórmula no se guarda aquí. */
    @Column(name = "formula_id", nullable = false, length = 20)
    private String formulaId;

    /** Versión de la fórmula que se evaluó; nula en las revisiones anteriores a V5. */
    @Column(name = "version_formula", length = 20)
    private String versionFormula;

    /** Nulo cuando la ejecuta el sistema (el PB lo muestra como "SISTEMA"). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigenRevision origen;

    @Column(name = "fecha_revision", nullable = false)
    private LocalDateTime fechaRevision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResultadoRevision resultado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> parametros;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;
}
