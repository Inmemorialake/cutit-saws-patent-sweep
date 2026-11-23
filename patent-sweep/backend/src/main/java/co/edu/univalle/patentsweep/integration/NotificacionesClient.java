package co.edu.univalle.patentsweep.integration;

import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.dto.NotificacionRequestDTO;
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
 * Cliente de la API de Notificaciones (P09). Reintenta solo ante timeout o error 5xx; un 4xx
 * no se reintenta (PB HU-12, criterios 4.1 y 4.2).
 */
@Component
public class NotificacionesClient {

    private final RestClient restClient;

    public NotificacionesClient(@Qualifier("notificacionesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** POST /api/v1/notificaciones: envía la alerta de un conflicto nuevo (PB HU-12). */
    @Retry(name = "notificaciones")
    @CircuitBreaker(name = "notificaciones")
    public void enviar(NotificacionRequestDTO notificacion) {
        try {
            restClient.post()
                    .uri("/api/v1/notificaciones")
                    .body(notificacion)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new IntegracionExternaException("Notificaciones rechazó la alerta: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new ServicioNoDisponibleException("No se pudo comunicar con el servicio de Notificaciones", e);
        } catch (RestClientException e) {
            throw new IntegracionExternaException("Respuesta inválida de Notificaciones", e);
        }
    }
}
