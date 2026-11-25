package co.edu.univalle.patentsweep.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check público (P28) en el puerto de la API. Actuator se sirve en un puerto de management
 * interno (ADR-010), así que este controlador mantiene {@code /actuator/health} donde siempre estuvo:
 * lo usan el healthcheck de Docker, el despliegue por SSM y la documentación. Solo expone el estado.
 */
@RestController
@Tag(name = "Salud", description = "Health check público de la API")
public class SaludController {

    private final HealthEndpoint healthEndpoint;

    public SaludController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    /** GET /actuator/health: 200 con {"status":"UP"}, o 503 con el estado si algo falla (p. ej. la BD). */
    @GetMapping("/actuator/health")
    @Operation(summary = "Estado de la API", description = "Refleja la salud de la API y de PostgreSQL.")
    @ApiResponse(responseCode = "200", description = "La API está disponible")
    @ApiResponse(responseCode = "503", description = "La API o alguna dependencia no está disponible")
    public ResponseEntity<Map<String, String>> salud() {
        Status estado = healthEndpoint.health().getStatus();
        HttpStatus codigo = Status.UP.equals(estado) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(codigo).body(Map.of("status", estado.getCode()));
    }
}
