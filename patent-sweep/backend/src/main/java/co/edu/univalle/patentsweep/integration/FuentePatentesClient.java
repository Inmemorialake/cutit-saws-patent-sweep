package co.edu.univalle.patentsweep.integration;

import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.integration.port.FuentePatentesPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP de la fuente externa de patentes (en desarrollo, la API simulada con WireMock).
 * Traduce los errores HTTP a excepciones propias y se protege con Retry (3 intentos, 5 s)
 * y Circuit Breaker.
 */
@Component
public class FuentePatentesClient implements FuentePatentesPort {

    private final RestClient restClient;

    public FuentePatentesClient(@Qualifier("fuentePatentesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** GET /patentes/buscar?terminos=a,b&clasificaciones=x,y&fechaSolicitudDesde=aaaa-mm-dd. */
    @Override
    @Retry(name = "fuentePatentes")
    @CircuitBreaker(name = "fuentePatentes")
    public List<PatenteExternaDTO> buscarPatentes(List<String> terminos, List<String> clasificaciones,
                                                  LocalDate fechaSolicitudDesde) {
        try {
            List<PatenteExternaDTO> patentes = restClient.get()
                    .uri(uri -> {
                        uri.path("/patentes/buscar");
                        if (terminos != null && !terminos.isEmpty()) {
                            uri.queryParam("terminos", String.join(",", terminos));
                        }
                        if (clasificaciones != null && !clasificaciones.isEmpty()) {
                            uri.queryParam("clasificaciones", String.join(",", clasificaciones));
                        }
                        if (fechaSolicitudDesde != null) {
                            uri.queryParam("fechaSolicitudDesde", fechaSolicitudDesde);
                        }
                        return uri.build();
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PatenteExternaDTO>>() { });
            if (patentes == null) {
                throw new ServicioNoDisponibleException("La fuente de patentes respondió sin contenido");
            }
            return patentes;
        } catch (HttpClientErrorException e) {
            throw new IntegracionExternaException("La fuente de patentes rechazó la consulta", e);
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new ServicioNoDisponibleException("No se pudo comunicar con la fuente de patentes", e);
        } catch (RestClientException e) {
            throw new IntegracionExternaException("Respuesta ilegible de la fuente de patentes", e);
        }
    }
}
