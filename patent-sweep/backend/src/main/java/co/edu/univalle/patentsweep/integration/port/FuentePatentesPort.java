package co.edu.univalle.patentsweep.integration.port;

import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import java.time.LocalDate;
import java.util.List;

/**
 * Puerto de la fuente externa de patentes. La lógica de negocio
 * depende sólo de esta interfaz; se puede cambiar el adaptador sin tocar el motor de coincidencias.
 */
public interface FuentePatentesPort {

    /**
     * Busca patentes candidatas.
     *
     * @param terminos nombres de materiales y aplicación técnica de la fórmula
     * @param clasificaciones códigos IPC/CPC activos en la configuración
     * @param fechaSolicitudDesde fecha mínima de solicitud, o {@code null} si no se limita por fecha
     */
    List<PatenteExternaDTO> buscarPatentes(List<String> terminos, List<String> clasificaciones,
                                           LocalDate fechaSolicitudDesde);
}
