package co.edu.univalle.patentsweep.model;

import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Coincidencia entre una fórmula y una patente (ER: Conflicto; única por fórmula, patente y versión, DA-17). */
@Entity
@Table(name = "conflicto",
        uniqueConstraints = @UniqueConstraint(name = "uq_conflicto_formula_patente_version",
                columnNames = {"formula_id", "patente_id", "version_formula"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conflicto {

    /** Identidad numérica de 6 dígitos (la secuencia inicia en 100000). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conflicto_id")
    private Long conflictoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private RevisionFormula revision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "formula_id", nullable = false)
    private Formula formula;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patente_id", nullable = false)
    private Patente patente;

    /** Versión de la fórmula evaluada cuando se detectó el conflicto (DA-17). */
    @Column(name = "version_formula", nullable = false, length = 20)
    private String versionFormula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_descarte_id")
    private Usuario usuarioDescarte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConflicto estado;

    @Column(name = "fecha_deteccion", nullable = false)
    private LocalDateTime fechaDeteccion;

    @Column(name = "motivo_descarte", columnDefinition = "TEXT")
    private String motivoDescarte;

    @Column(name = "fecha_descarte")
    private LocalDateTime fechaDescarte;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        createdAt = ahora;
        updatedAt = ahora;
    }

    @PreUpdate
    void alActualizar() {
        updatedAt = LocalDateTime.now();
    }
}
