package co.edu.univalle.patentsweep.integration.dto;

import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;

/** Patente candidata devuelta por la fuente externa de patentes. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PatenteExternaDTO(
        String patenteId,
        String tituloPatente,
        String tipoProducto,
        EstadoVigenciaPatente estadoPatente,
        LocalDate fechaSolicitud,
        LocalDate fechaConcesion,
        LocalDate fechaVencimiento,
        String jurisdiccion,
        String resumen,
        List<String> reivindicaciones,
        List<String> clasificaciones) {
}
