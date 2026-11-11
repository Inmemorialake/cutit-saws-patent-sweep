package co.edu.univalle.patentsweep.model;

import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Patente candidata detectada en la fuente externa (ER: Patente). */
@Entity
@Table(name = "patente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patente_id")
    private Long patenteId;

    /** Código de negocio con formato PAT-NNNNNN. */
    @Pattern(regexp = "^PAT-\\d{6}$", message = "El código de la patente debe tener el formato PAT-NNNNNN")
    @Column(name = "codigo_patente", nullable = false, unique = true, length = 100)
    private String codigoPatente;

    @NotBlank(message = "El título de la patente es obligatorio")
    @Size(max = 255, message = "El título no puede superar 255 caracteres")
    @Column(nullable = false, length = 255)
    private String titulo;

    @Size(max = 60, message = "El tipo de producto no puede superar 60 caracteres")
    @Column(name = "tipo_producto", length = 60)
    private String tipoProducto;

    @Column(columnDefinition = "TEXT")
    private String resumen;

    /** Reivindicaciones separadas por salto de línea. */
    @Column(columnDefinition = "TEXT")
    private String reivindicaciones;

    @Column(name = "fecha_solicitud")
    private LocalDate fechaSolicitud;

    @Column(name = "fecha_concesion")
    private LocalDate fechaConcesion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Size(max = 50, message = "La jurisdicción no puede superar 50 caracteres")
    @Column(length = 50)
    private String jurisdiccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_vigencia", length = 30)
    private EstadoVigenciaPatente estadoVigencia;

    @Column(length = 100)
    private String fuente;

    @Column(name = "url_fuente", columnDefinition = "TEXT")
    private String urlFuente;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void alCrear() {
        createdAt = LocalDateTime.now();
    }
}
