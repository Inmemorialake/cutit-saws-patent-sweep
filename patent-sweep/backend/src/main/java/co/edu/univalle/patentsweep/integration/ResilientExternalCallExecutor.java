package co.edu.univalle.patentsweep.integration;

import co.edu.univalle.patentsweep.config.AsyncConfig;
import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Ejecuta integraciones bloqueantes de forma asíncrona para aplicar un timeout global
 * que incluya todos los reintentos de una llamada lógica.
 */
@Component
public class ResilientExternalCallExecutor {

    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final Executor integracionExecutor;
    private final ScheduledExecutorService resilienceScheduler;

    public ResilientExternalCallExecutor(RetryRegistry retryRegistry,
                                         CircuitBreakerRegistry circuitBreakerRegistry,
                                         TimeLimiterRegistry timeLimiterRegistry,
                                         @Qualifier(AsyncConfig.EJECUTOR_INTEGRACIONES) Executor integracionExecutor,
                                         @Qualifier(AsyncConfig.EJECUTOR_RESILIENCIA)
                                         ScheduledExecutorService resilienceScheduler) {
        this.retryRegistry = retryRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.integracionExecutor = integracionExecutor;
        this.resilienceScheduler = resilienceScheduler;
    }

    public <T> T ejecutarMateriales(Supplier<T> operacion) {
        return ejecutar("materiales", "Materiales/Fórmulas", operacion);
    }

    public <T> T ejecutarFuentePatentes(Supplier<T> operacion) {
        return ejecutar("fuentePatentes", "la fuente de patentes", operacion);
    }

    private <T> T ejecutar(String nombre, String servicio, Supplier<T> operacion) {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(nombre);
        long plazo = System.nanoTime() + timeLimiter.getTimeLimiterConfig().getTimeoutDuration().toNanos();

        Supplier<T> operacionConPlazo = () -> {
            if (System.nanoTime() - plazo >= 0) {
                throw new CancellationException("Venció el límite global de la integración");
            }
            return operacion.get();
        };
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(nombre);
        Supplier<T> protegidaPorCircuitBreaker =
                CircuitBreaker.decorateSupplier(circuitBreaker, operacionConPlazo);
        Retry retry = retryRegistry.retry(nombre);
        Supplier<T> conReintentos = Retry.decorateSupplier(retry, protegidaPorCircuitBreaker);
        Supplier<CompletionStage<T>> llamadaAsincrona = () -> ejecutarAsync(conReintentos);
        CompletionStage<T> limitada =
                timeLimiter.executeCompletionStage(resilienceScheduler, llamadaAsincrona);

        return esperar(limitada, servicio);
    }

    private <T> CompletionStage<T> ejecutarAsync(Supplier<T> operacion) {
        try {
            return CompletableFuture.supplyAsync(operacion, integracionExecutor);
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(
                    new ServicioNoDisponibleException("No hay capacidad disponible para la integración externa", e));
        }
    }

    private static <T> T esperar(CompletionStage<T> llamada, String servicio) {
        try {
            return llamada.toCompletableFuture().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException("Se interrumpió la consulta a " + servicio, e);
        } catch (ExecutionException e) {
            Throwable causa = causaRaiz(e);
            if (causa instanceof TimeoutException) {
                throw new ServicioNoDisponibleException(
                        "Se agotó el límite global de espera para " + servicio, causa);
            }
            if (causa instanceof CancellationException) {
                throw new ServicioNoDisponibleException("Se canceló la consulta a " + servicio, causa);
            }
            if (causa instanceof RuntimeException excepcion) {
                throw excepcion;
            }
            if (causa instanceof Error error) {
                throw error;
            }
            throw new IntegracionExternaException("Falló la consulta a " + servicio, causa);
        } catch (CancellationException e) {
            throw new ServicioNoDisponibleException("Se canceló la consulta a " + servicio, e);
        }
    }

    private static Throwable causaRaiz(Throwable causa) {
        Throwable actual = causa;
        while ((actual instanceof ExecutionException || actual instanceof CompletionException)
                && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
    }
}
