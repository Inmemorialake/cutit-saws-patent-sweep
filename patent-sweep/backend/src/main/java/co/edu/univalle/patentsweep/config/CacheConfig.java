package co.edu.univalle.patentsweep.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caché en memoria con Caffeine (P15 Cache-aside). Queda lista para el catálogo IPC/CPC que
 * validarán HU-02 y HU-10; las notificaciones no forman parte de este sprint.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CATALOGO_CLASIFICACIONES = "clasificaciones";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager gestor = new CaffeineCacheManager(CATALOGO_CLASIFICACIONES);
        gestor.setCaffeine(Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(1_000).recordStats());
        return gestor;
    }
}
