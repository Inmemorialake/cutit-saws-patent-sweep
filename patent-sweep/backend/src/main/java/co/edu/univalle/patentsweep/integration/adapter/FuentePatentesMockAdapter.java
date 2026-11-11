package co.edu.univalle.patentsweep.integration.adapter;

import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.integration.port.FuentePatentesPort;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adaptador Mock robusto para la fuente externa de patentes.
 * Combina respuestas preconfiguradas con lógica de filtrado por clasificación,
 * fechas y términos para respaldar las pruebas unitarias y el flujo de la transacción.
 */
@Component
public class FuentePatentesMockAdapter implements FuentePatentesPort {

    @Override
    @Retry(name = "fuentePatentes")
    @CircuitBreaker(name = "fuentePatentes")
    public List<PatenteExternaDTO> buscarPatentes(List<String> terminos, List<String> clasificaciones, LocalDate fechaSolicitudDesde) {

        List<String> terminosMinuscula = terminos == null ? List.of() :
                terminos.stream().map(t -> t.toLowerCase(Locale.ROOT)).toList();

        // Escenario 1: Fallo técnico simulado (HTTP 503).
        if (terminosMinuscula.contains("error-tecnico") || terminosMinuscula.contains("fallo")) {
            throw new RuntimeException("Falla simulada en la red de la fuente externa de patentes");
        }

        // Escenario 2: Fórmula limpia (Sin conflictos).
        if (terminosMinuscula.contains("formula-limpia") || terminosMinuscula.contains("limpia")) {
            return List.of();
        }

        // Dataset preconfigurado interno en memoria.
        List<PatenteExternaDTO> datasetPreconfigurado = List.of(
                new PatenteExternaDTO(
                        "PAT-2026-999",
                        "Cuchilla hidráulica de diamante reforzado para corte de alta precisión",
                        "Industrial",
                        EstadoVigenciaPatente.CONCEDIDA,
                        LocalDate.of(2025, 3, 15),
                        LocalDate.of(2025, 1, 10),
                        LocalDate.of(2035, 1, 10),
                        "CO",
                        "Dispositivo de corte industrial enfocado en cobalto y aleaciones de alta resistencia.",
                        List.of("Reivindicación 1: Cuchilla con recubrimiento de diamante.", "Reivindicación 2: Sistema hidráulico acoplado."),
                        List.of("B23D", "B24D 3/00")
                ),
                new PatenteExternaDTO(
                        "PAT-2025-888",
                        "Aleación especial de cobalto para sierras mecánicas industriales",
                        "Materiales",
                        EstadoVigenciaPatente.CONCEDIDA,
                        LocalDate.of(2024, 10, 10),
                        LocalDate.of(2024, 3, 20),
                        LocalDate.of(2034, 3, 20),
                        "US",
                        "Aleación metálica basada en cobalto optimizada para fricción extrema.",
                        List.of("Reivindicación 1: Composición química de cobalto y cromo."),
                        List.of("C22C", "B24D 3/00")
                ),
                new PatenteExternaDTO(
                        "PAT-2024-111",
                        "Sistema de poleas y transmisión para motores de corte",
                        "Mecánica",
                        EstadoVigenciaPatente.EN_TRAMITE,
                        LocalDate.of(2022, 1, 15),
                        null,
                        null,
                        "EP",
                        "Mecanismo de transmisión de potencia para maquinaria pesada de corte.",
                        List.of("Reivindicación 1: Ensamblaje de poleas dentadas."),
                        List.of("F16H")
                ),
                new PatenteExternaDTO(
                        "PAT-2025-333",
                        "Aleación para cadenas de corte",
                        "Materiales",
                        EstadoVigenciaPatente.CONCEDIDA,
                        LocalDate.of(2025, 1, 10),
                        LocalDate.of(2026, 6, 22),
                        LocalDate.of(2031, 1, 10),
                        "US",
                        "Aleación metálica basada en Hierro, Cobalto y Níquel para cadenas de corte.",
                        List.of("Reivindicación 1: Composición con Hierro, Cobalto y Níquel."),
                        List.of("B27B 17/00", "B24D 3/00")
                )
        );

        // Filtrado dinámico sobre el dataset preconfigurado.
        return datasetPreconfigurado.stream()
                .filter(patente -> coincideClasificacion(patente, clasificaciones))
                .filter(patente -> cumpleRangoFecha(patente, fechaSolicitudDesde))
                .filter(patente -> mencionaAlgunTermino(patente, terminosMinuscula))
                .toList();
    }

    private static boolean coincideClasificacion(PatenteExternaDTO patente, List<String> clasificaciones) {
        if (clasificaciones == null || clasificaciones.isEmpty()) {
            return true;
        }
        return patente.clasificaciones() != null
                && patente.clasificaciones().stream().anyMatch(clasificaciones::contains);
    }

    private static boolean cumpleRangoFecha(PatenteExternaDTO patente, LocalDate desde) {
        if (desde == null) {
            return true;
        }
        return patente.fechaSolicitud() != null && !patente.fechaSolicitud().isBefore(desde);
    }

    private static boolean mencionaAlgunTermino(PatenteExternaDTO patente, List<String> terminosMinuscula) {
        if (terminosMinuscula == null || terminosMinuscula.isEmpty()) {
            return true;
        }
        String reivindicaciones = patente.reivindicaciones() == null
                ? "" : String.join(" ", patente.reivindicaciones());
        String texto = (Objects.toString(patente.tituloPatente(), "") + " "
                + Objects.toString(patente.resumen(), "") + " " + reivindicaciones).toLowerCase(Locale.ROOT);
        return terminosMinuscula.stream().anyMatch(texto::contains);
    }
}
