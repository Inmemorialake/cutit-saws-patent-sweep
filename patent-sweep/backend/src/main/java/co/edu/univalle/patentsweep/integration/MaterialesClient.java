package co.edu.univalle.patentsweep.integration;

import co.edu.univalle.patentsweep.exception.FormulaNoEncontradaException;
import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaResumenDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente de Materiales/Fórmulas. Traduce los errores HTTP a excepciones
 * propias y se protege con Retry (3 intentos, 5 s) y Circuit Breaker.
 */
@Component
public class MaterialesClient {

    private static final int TAMANO_PAGINA = 10;

    private final RestClient restClient;
    private final ResilientExternalCallExecutor llamadas;

    public MaterialesClient(@Qualifier("materialesRestClient") RestClient restClient,
                            ResilientExternalCallExecutor llamadas) {
        this.restClient = restClient;
        this.llamadas = llamadas;
    }

    /** GET /formulas/{codigo}: detalle de una fórmula con sus componentes. */
    public FormulaExternaDTO obtenerFormula(String formulaId) {
        return llamadas.ejecutarMateriales(() -> obtenerFormulaRemota(formulaId));
    }

    /** Igual que {@link #obtenerFormula} pero con el límite de 5 s y sin reintentos que pide el PB en HU-04. */
    public FormulaExternaDTO verificarFormula(String formulaId) {
        return llamadas.ejecutarVerificacionMateriales(() -> obtenerFormulaRemota(formulaId));
    }

    private FormulaExternaDTO obtenerFormulaRemota(String formulaId) {
        try {
            FormulaExternaDTO formula = restClient.get()
                    .uri("/formulas/{codigo}", formulaId)
                    .retrieve()
                    .body(FormulaExternaDTO.class);
            if (formula == null) {
                throw new ServicioNoDisponibleException("Materiales/Fórmulas respondió sin contenido");
            }
            return formula;
        } catch (HttpClientErrorException.NotFound e) {
            throw new FormulaNoEncontradaException();
        } catch (HttpClientErrorException e) {
            throw new IntegracionExternaException("Materiales/Fórmulas rechazó la consulta", e);
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new ServicioNoDisponibleException("No se pudo comunicar con el servicio de Materiales/Fórmulas", e);
        } catch (RestClientException e) {
            throw new IntegracionExternaException("Respuesta ilegible de Materiales/Fórmulas", e);
        }
    }

    /**
     * HU-01-1B: recorre `GET /formulas?estado=APROBADA&pagina={n}` en páginas de 10 hasta recibir
     * una lista vacía. El tamaño es fijo en código (D-2); el llamador no puede cambiarlo.
     */
    public List<FormulaResumenDTO> listarAprobadas() {
        return llamadas.ejecutarMateriales(this::listarAprobadasRemoto);
    }

    private List<FormulaResumenDTO> listarAprobadasRemoto() {
        List<FormulaResumenDTO> aprobadas = new ArrayList<>();
        int pagina = 1;
        try {
            while (true) {
                int paginaActual = pagina;
                List<FormulaResumenDTO> lote = restClient.get()
                        .uri(uri -> uri.path("/formulas")
                                .queryParam("estado", "APROBADA")
                                .queryParam("pagina", paginaActual)
                                .build())
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() { });
                if (lote == null || lote.isEmpty()) {
                    return aprobadas;
                }
                aprobadas.addAll(lote);
                pagina++;
            }
        } catch (HttpClientErrorException e) {
            throw new IntegracionExternaException("Materiales/Fórmulas rechazó la consulta", e);
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new ServicioNoDisponibleException("No se pudo comunicar con el servicio de Materiales/Fórmulas", e);
        } catch (RestClientException e) {
            throw new IntegracionExternaException("Respuesta ilegible de Materiales/Fórmulas", e);
        }
    }
}
