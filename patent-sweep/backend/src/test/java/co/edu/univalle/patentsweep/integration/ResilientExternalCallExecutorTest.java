package co.edu.univalle.patentsweep.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Test
    @DisplayName("La verificación de HU-04 no reintenta: un fallo se propaga tras un solo intento")
    void noDeberiaReintentarLaVerificacionDeMateriales() {
        ResilientExternalCallExecutor executor = crearExecutor(Duration.ofSeconds(2), Duration.ofMillis(10));
        AtomicInteger llamadas = new AtomicInteger();

        assertThatThrownBy(() -> executor.ejecutarVerificacionMateriales(() -> {
            llamadas.incrementAndGet();
            throw new ServicioNoDisponibleException("Fallo simulado");
        })).isInstanceOf(ServicioNoDisponibleException.class);

        assertThat(llamadas).hasValue(1);
    }

    @Test
    @DisplayName("Ejecuta Materiales y devuelve el resultado exitoso")
    void deberiaDevolverResultadoDeMateriales() {
        ResilientExternalCallExecutor executor = crearExecutor(Duration.ofSeconds(2), Duration.ofMillis(10));

        String resultado = executor.ejecutarMateriales(() -> "respuesta");

        assertThat(resultado).isEqualTo("respuesta");
    }

    @Test
    @DisplayName("Traduce el rechazo del executor a servicio no disponible")
    void deberiaTraducirElRechazoDelExecutor() {
        Executor executorRechazado = tarea -> {
            throw new RejectedExecutionException("Pool lleno");
        };
        ResilientExternalCallExecutor executor =
                crearExecutor(Duration.ofSeconds(2), Duration.ofMillis(10), executorRechazado);

        assertThatThrownBy(() -> executor.ejecutarMateriales(() -> "respuesta"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("No hay capacidad disponible")
                .hasCauseInstanceOf(RejectedExecutionException.class);
    }

    @Test
    @DisplayName("Propaga excepciones de ejecución no reintentables")
    void deberiaPropagarUnaExcepcionDeEjecucion() {
        ResilientExternalCallExecutor executor = crearExecutor(Duration.ofSeconds(2), Duration.ofMillis(10));
        IllegalArgumentException esperada = new IllegalArgumentException("Argumento inválido");

        assertThatThrownBy(() -> executor.ejecutarFuentePatentes(() -> {
            throw esperada;
        })).isSameAs(esperada);
    }

    @Test
    @DisplayName("Convierte la cancelación en servicio no disponible")
    void deberiaTraducirLaCancelacion() {
        ResilientExternalCallExecutor executor = crearExecutor(Duration.ofSeconds(2), Duration.ofMillis(10));

        assertThatThrownBy(() -> executor.ejecutarFuentePatentes(() -> {
            throw new CancellationException("Cancelación simulada");
        }))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("canceló la consulta");
    }

    @Test
    @DisplayName("Propaga los errores de ejecución")
    void deberiaPropagarUnErrorDeEjecucion() {
        ResilientExternalCallExecutor executor = crearExecutor(Duration.ofSeconds(2), Duration.ofMillis(10));
        AssertionError esperado = new AssertionError("Error simulado");

        assertThatThrownBy(() -> executor.ejecutarFuentePatentes(() -> {
            throw esperado;
        })).isSameAs(esperado);
    }

    @Test
    @DisplayName("Envuelve una causa comprobada en una excepción de integración")
    void deberiaEnvolverUnaCausaComprobada() {
        ResilientExternalCallExecutor executor = crearExecutor(Duration.ofSeconds(2), Duration.ofMillis(10));

        assertThatThrownBy(() -> executor.ejecutarFuentePatentes(() -> {
            throw new CompletionException(new IOException("Fallo comprobado"));
        }))
                .isInstanceOf(IntegracionExternaException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Conserva una CompletionException sin causa")
    void deberiaPropagarUnaCompletionExceptionSinCausa() {
        ResilientExternalCallExecutor executor = crearExecutor(Duration.ofSeconds(2), Duration.ofMillis(10));
        CompletionException esperada = new CompletionException((Throwable) null);

        assertThatThrownBy(() -> executor.ejecutarFuentePatentes(() -> {
            throw esperada;
        })).isSameAs(esperada);
    }

    @Test
    @DisplayName("Restaura la interrupción del hilo que espera la integración")
    void deberiaRestaurarLaInterrupcionDelHilo() throws InterruptedException {
        ResilientExternalCallExecutor executor = crearExecutor(Duration.ofSeconds(2), Duration.ofMillis(10));
        CountDownLatch operacionIniciada = new CountDownLatch(1);
        CountDownLatch liberarOperacion = new CountDownLatch(1);
        AtomicBoolean interrupcionRestaurada = new AtomicBoolean();

        FutureTask<Void> tarea = new FutureTask<>(() -> {
            try {
                executor.ejecutarMateriales(() -> {
                    operacionIniciada.countDown();
                    try {
                        liberarOperacion.await();
                        return "respuesta";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ServicioNoDisponibleException("Se interrumpió la operación de prueba", e);
                    }
                });
                return null;
            } finally {
                interrupcionRestaurada.set(Thread.currentThread().isInterrupted());
            }
        });
        Thread solicitante = new Thread(tarea);

        solicitante.start();
        try {
            assertThat(operacionIniciada.await(1, TimeUnit.SECONDS)).isTrue();
            solicitante.interrupt();
            solicitante.join(1000);

            assertThat(solicitante.isAlive()).isFalse();
            assertThatThrownBy(tarea::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ServicioNoDisponibleException.class);
            assertThat(interrupcionRestaurada.get()).isTrue();
        } finally {
            liberarOperacion.countDown();
            solicitante.interrupt();
            solicitante.join(1000);
        }
    }

    private ResilientExternalCallExecutor crearExecutor(Duration timeout, Duration esperaReintento) {
        integracionExecutor = Executors.newFixedThreadPool(2);
        return crearExecutor(timeout, esperaReintento, integracionExecutor);
    }

    private ResilientExternalCallExecutor crearExecutor(Duration timeout, Duration esperaReintento,
                                                        Executor executor) {
        resilienceScheduler = Executors.newScheduledThreadPool(2);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(esperaReintento)
                .retryExceptions(ServicioNoDisponibleException.class)
                .build();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        retryRegistry.retry("materiales", retryConfig);
        retryRegistry.retry("fuentePatentes", retryConfig);
        retryRegistry.retry("materialesVerificacion", RetryConfig.custom()
                .maxAttempts(1)
                .retryExceptions(ServicioNoDisponibleException.class)
                .build());

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(timeout)
                .cancelRunningFuture(true)
                .build();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        timeLimiterRegistry.timeLimiter("materiales", timeLimiterConfig);
        timeLimiterRegistry.timeLimiter("fuentePatentes", timeLimiterConfig);
        timeLimiterRegistry.timeLimiter("materialesVerificacion", timeLimiterConfig);

        return new ResilientExternalCallExecutor(retryRegistry, CircuitBreakerRegistry.ofDefaults(),
                timeLimiterRegistry, executor, resilienceScheduler);
    }
}
