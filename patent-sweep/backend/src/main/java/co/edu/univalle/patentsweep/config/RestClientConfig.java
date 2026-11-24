package co.edu.univalle.patentsweep.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Un RestClient por API externa. Los timeouts HTTP limitan cada intento; Resilience4j aplica
 * aparte el timeout global de la llamada lógica, incluidos sus reintentos.
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
