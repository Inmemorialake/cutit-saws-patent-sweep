package co.edu.univalle.patentsweep.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Un RestClient por API externa. El tiempo de lectura (20 s por intento, según el PB) hace de
 * TimeLimiter: si la API no responde, el intento falla y Resilience4j lo reintenta.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient materialesRestClient(RestClient.Builder constructor,
                                           @Value("${patentsweep.materiales.url-base}") String urlBase,
                                           @Value("${patentsweep.http.timeout-conexion-segundos}") long conexion,
                                           @Value("${patentsweep.http.timeout-lectura-segundos}") long lectura) {
        return construir(constructor, urlBase, conexion, lectura);
    }

    @Bean
    public RestClient notificacionesRestClient(RestClient.Builder constructor,
                                               @Value("${patentsweep.notificaciones.url-base}") String urlBase,
                                               @Value("${patentsweep.http.timeout-conexion-segundos}") long conexion,
                                               @Value("${patentsweep.http.timeout-lectura-segundos}") long lectura) {
        return construir(constructor, urlBase, conexion, lectura);
    }

    @Bean
    public RestClient fuentePatentesRestClient(RestClient.Builder constructor,
                                               @Value("${patentsweep.fuente-patentes.url-base}") String urlBase,
                                               @Value("${patentsweep.http.timeout-conexion-segundos}") long conexion,
                                               @Value("${patentsweep.http.timeout-lectura-segundos}") long lectura) {
        return construir(constructor, urlBase, conexion, lectura);
    }

    private static RestClient construir(RestClient.Builder constructor, String urlBase, long conexion, long lectura) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(conexion));
        fabrica.setReadTimeout(Duration.ofSeconds(lectura));
        return constructor.baseUrl(urlBase).requestFactory(fabrica).build();
    }
}
