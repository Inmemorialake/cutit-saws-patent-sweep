package co.edu.univalle.patentsweep.controller.mapper;

import co.edu.univalle.patentsweep.controller.dto.BusquedaResponseDTO;
import co.edu.univalle.patentsweep.controller.dto.ConflictoResumenDTO;
import co.edu.univalle.patentsweep.model.Conflicto;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Traduce entidades a DTO (P05): al cliente solo se exponen los campos necesarios, nunca la entidad.
 * [Archetype Convention Addition] Mapeo manual para no sumar MapStruct, que no está en el DAS.
 */
@Component
public class ConflictoMapper {

    static final int MAXIMO_TITULO = 60;
    static final String MENSAJE_SIN_CONFLICTOS = "No se detectaron conflictos relacionados con su búsqueda";
    static final String MENSAJE_CON_CONFLICTOS = "Búsqueda completada correctamente";

    public ConflictoResumenDTO aResumen(Conflicto conflicto) {
        String titulo = conflicto.getPatente().getTitulo();
        return new ConflictoResumenDTO(
                String.format("%06d", conflicto.getConflictoId()),
                titulo.length() > MAXIMO_TITULO ? titulo.substring(0, MAXIMO_TITULO) : titulo,
                conflicto.getPatente().getTipoProducto(),
                conflicto.getPatente().getEstadoVigencia(),
                conflicto.getPatente().getFechaConcesion());
    }

    public BusquedaResponseDTO aRespuestaDeBusqueda(String formulaId, List<Conflicto> conflictos) {
        List<ConflictoResumenDTO> filas = conflictos.stream().map(this::aResumen).toList();
        return new BusquedaResponseDTO(formulaId, filas,
                filas.isEmpty() ? MENSAJE_SIN_CONFLICTOS : MENSAJE_CON_CONFLICTOS);
    }
}
