package co.edu.univalle.patentsweep.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Ejecutor propio para las tareas asíncronas (notificaciones), para que no compitan con los
 * hilos que atienden las peticiones REST (DAS, tabla 14).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String EJECUTOR_NOTIFICACIONES = "notificacionExecutor";

    @Bean(name = EJECUTOR_NOTIFICACIONES)
    public Executor notificacionExecutor(@Value("${patentsweep.async.nucleo}") int nucleo,
                                         @Value("${patentsweep.async.maximo}") int maximo,
                                         @Value("${patentsweep.async.cola}") int cola) {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(nucleo);
        ejecutor.setMaxPoolSize(maximo);
        ejecutor.setQueueCapacity(cola);
        ejecutor.setThreadNamePrefix("notificacion-");
        return ejecutor;
    }
}
