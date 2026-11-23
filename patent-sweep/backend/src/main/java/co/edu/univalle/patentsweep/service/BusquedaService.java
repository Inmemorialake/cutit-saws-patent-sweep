package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.exception.ConfiguracionNoEncontradaException;
import co.edu.univalle.patentsweep.exception.FormulaDescontinuadaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.MaterialesClient;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.integration.port.FuentePatentesPort;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Configuracion;
import co.edu.univalle.patentsweep.repository.ConfiguracionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Búsqueda manual de patentes en conflicto con una fórmula. Orquesta la consulta a
 * Materiales, la búsqueda en la fuente y el motor de coincidencias; la persistencia la hace
 * {@link RevisionService} en una transacción corta, para no retener una conexión durante las llamadas HTTP.
 */
@Service
public class BusquedaService {

    private static final String UNIDAD_MESES = "MESES";

    private final MaterialesClient materialesClient;
    private final FuentePatentesPort fuentePatentes;
    private final MotorCoincidencias motorCoincidencias;
    private final ConfiguracionRepository configuracionRepository;
    private final RevisionService revisionService;
    private final Clock reloj;

    public BusquedaService(MaterialesClient materialesClient, FuentePatentesPort fuentePatentes,
                           MotorCoincidencias motorCoincidencias, ConfiguracionRepository configuracionRepository,
                           RevisionService revisionService, Clock reloj) {
        this.materialesClient = materialesClient;
        this.fuentePatentes = fuentePatentes;
        this.motorCoincidencias = motorCoincidencias;
        this.configuracionRepository = configuracionRepository;
        this.revisionService = revisionService;
        this.reloj = reloj;
    }

    /**
     * @param formulaId código de la fórmula, con formato form-NNN
     * @param username usuario autenticado, tomado del token de la sesión
     * @return conflictos de las patentes coincidentes; vacío si no hubo coincidencias
     */
    public List<Conflicto> buscar(String formulaId, String username) {
        FormulaExternaDTO formula = materialesClient.obtenerFormula(formulaId);
        if (formula.estaDescontinuada()) {
            throw new FormulaDescontinuadaException();
        }
        Configuracion configuracion = configuracionRepository.findByActivaTrue()
                .orElseThrow(ConfiguracionNoEncontradaException::new);
        LocalDate desde = calcularFechaDesde(configuracion);
        List<String> terminos = motorCoincidencias.terminosDeBusqueda(formula);
        Map<String, Object> parametros = armarParametros(configuracion, desde, formula, terminos);

        List<PatenteExternaDTO> candidatas;
        try {
            candidatas = fuentePatentes.buscarPatentes(terminos, configuracion.getClasificaciones(), desde);
        } catch (RuntimeException e) {
            revisionService.registrarFalloTecnico(formula, username, e.getMessage(), parametros);
            throw new ServicioNoDisponibleException("No se pudo comunicar con la fuente externa de patentes", e);
        }
        parametros.put("patentesCandidatas", candidatas.stream().map(PatenteExternaDTO::patenteId).toList());
        return revisionService.registrarBusqueda(
                formula, username, motorCoincidencias.filtrarConflictos(formula, candidatas), parametros);
    }

    /** El rango relativo se cuenta hacia atrás desde hoy y se aplica a la fecha de solicitud. */
    private LocalDate calcularFechaDesde(Configuracion configuracion) {
        if (configuracion.getRangoCantidad() == null || configuracion.getRangoUnidad() == null) {
            return null;
        }
        LocalDate hoy = LocalDate.now(reloj);
        return UNIDAD_MESES.equals(configuracion.getRangoUnidad())
                ? hoy.minusMonths(configuracion.getRangoCantidad())
                : hoy.minusYears(configuracion.getRangoCantidad());
    }

    /** Evidencia de la revisión: foto de la fórmula, términos buscados y filtros usados. */
    private static Map<String, Object> armarParametros(Configuracion configuracion, LocalDate desde,
                                                       FormulaExternaDTO formula, List<String> terminos) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("formula", formula.foto());
        parametros.put("terminosBusqueda", terminos);
        parametros.put("clasificaciones", configuracion.getClasificaciones());
        parametros.put("fechaSolicitudDesde", desde == null ? null : desde.toString());
        return parametros;
    }
}
