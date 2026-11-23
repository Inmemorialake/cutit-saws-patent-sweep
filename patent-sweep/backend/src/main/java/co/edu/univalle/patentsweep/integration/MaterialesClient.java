package co.edu.univalle.patentsweep.integration;

import co.edu.univalle.patentsweep.exception.FormulaNoEncontradaException;
import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente de Materiales/Fórmulas. Traduce los errores HTTP a excepciones
 * propias y se protege con Retry (3 intentos, 5 s) y Circuit Breaker.
 */
@Component
public class MaterialesClient {

    private final RestClient restClient;

    public MaterialesClient(@Qualifier("materialesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** GET /formulas/{codigo}: detalle de una fórmula con sus componentes. */
    @Retry(name = "materiales")
    @CircuitBreaker(name = "materiales")
    public FormulaExternaDTO obtenerFormula(String formulaId) {
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
}
