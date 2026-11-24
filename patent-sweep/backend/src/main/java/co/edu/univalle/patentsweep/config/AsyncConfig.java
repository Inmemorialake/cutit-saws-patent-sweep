package co.edu.univalle.patentsweep.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Pools independientes para eventos de dominio e integraciones bloqueantes, sin competir con
 * los hilos que atienden las peticiones REST (DAS, tabla 14).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String EJECUTOR_NOTIFICACIONES = "notificacionExecutor";
    public static final String EJECUTOR_INTEGRACIONES = "integracionExecutor";
    public static final String EJECUTOR_RESILIENCIA = "resilienceScheduler";

    @Bean(name = EJECUTOR_NOTIFICACIONES)
    public Executor notificacionExecutor(@Value("${patentsweep.async.nucleo}") int nucleo,
                                         @Value("${patentsweep.async.maximo}") int maximo,
                                         @Value("${patentsweep.async.cola}") int cola) {
        return crearEjecutor(nucleo, maximo, cola, "notificacion-");
    }

    @Bean(name = EJECUTOR_INTEGRACIONES)
    public Executor integracionExecutor(@Value("${patentsweep.async.nucleo}") int nucleo,
                                        @Value("${patentsweep.async.maximo}") int maximo,
                                        @Value("${patentsweep.async.cola}") int cola) {
        return crearEjecutor(nucleo, maximo, cola, "integracion-");
    }

    @Bean(name = EJECUTOR_RESILIENCIA, destroyMethod = "shutdown")
    public ScheduledExecutorService resilienceScheduler() {
        return Executors.newScheduledThreadPool(2);
    }

    private static Executor crearEjecutor(int nucleo, int maximo, int cola, String prefijo) {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(nucleo);
        ejecutor.setMaxPoolSize(maximo);
        ejecutor.setQueueCapacity(cola);
        ejecutor.setThreadNamePrefix(prefijo);
        return ejecutor;
    }
}
