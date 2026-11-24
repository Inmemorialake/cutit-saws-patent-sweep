package co.edu.univalle.patentsweep.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResilientExternalCallExecutorTest {

    private ExecutorService integracionExecutor;
    private ScheduledExecutorService resilienceScheduler;

    @AfterEach
    void liberarEjecutores() {
        if (integracionExecutor != null) {
            integracionExecutor.shutdownNow();
        }
        if (resilienceScheduler != null) {
            resilienceScheduler.shutdownNow();
        }
    }

    @Test
    @DisplayName("El timeout global incluye la espera de reintento y evita otra llamada tardía")
    void deberiaDetenerLosReintentosAlVencerElTimeoutGlobal() throws InterruptedException {
        ResilientExternalCallExecutor executor = crearExecutor(Duration.ofMillis(100), Duration.ofMillis(300));
        AtomicInteger llamadas = new AtomicInteger();

        assertThatThrownBy(() -> executor.ejecutarMateriales(() -> {
            llamadas.incrementAndGet();
            throw new ServicioNoDisponibleException("Fallo simulado");
        }))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("límite global")
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);

        Thread.sleep(700);

        assertThat(llamadas).hasValue(1);
    }

    @Test
    @DisplayName("Reintenta los errores transitorios y devuelve el resultado exitoso")
    void deberiaReintentarAntesDelTimeoutGlobal() {
        ResilientExternalCallExecutor executor = crearExecutor(Duration.ofSeconds(2), Duration.ofMillis(10));
        AtomicInteger llamadas = new AtomicInteger();

        String resultado = executor.ejecutarFuentePatentes(() -> {
            if (llamadas.incrementAndGet() < 3) {
                throw new ServicioNoDisponibleException("Fallo transitorio");
            }
            return "respuesta";
        });

        assertThat(resultado).isEqualTo("respuesta");
        assertThat(llamadas).hasValue(3);
    }

    private ResilientExternalCallExecutor crearExecutor(Duration timeout, Duration esperaReintento) {
        integracionExecutor = Executors.newFixedThreadPool(2);
        resilienceScheduler = Executors.newScheduledThreadPool(2);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(esperaReintento)
                .retryExceptions(ServicioNoDisponibleException.class)
                .build();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        retryRegistry.retry("materiales", retryConfig);
        retryRegistry.retry("fuentePatentes", retryConfig);

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(timeout)
                .cancelRunningFuture(true)
                .build();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        timeLimiterRegistry.timeLimiter("materiales", timeLimiterConfig);
        timeLimiterRegistry.timeLimiter("fuentePatentes", timeLimiterConfig);

        return new ResilientExternalCallExecutor(retryRegistry, CircuitBreakerRegistry.ofDefaults(),
                timeLimiterRegistry, integracionExecutor, resilienceScheduler);
    }
}
