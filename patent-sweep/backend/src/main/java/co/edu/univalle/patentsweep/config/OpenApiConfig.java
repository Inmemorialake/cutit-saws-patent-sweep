package co.edu.univalle.patentsweep.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/** Contrato OpenAPI (P24): Swagger UI en /swagger-ui.html y contrato en /v3/api-docs. */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "API Patent Sweep",
        version = "1.0.0",
        description = "Vigilancia de patentes sobre las fórmulas de cuchilla de Cutit Saws Ltd. "
                + "Arquetipo Sprint 0: registro y consulta de patentes."))
public class OpenApiConfig {
}
