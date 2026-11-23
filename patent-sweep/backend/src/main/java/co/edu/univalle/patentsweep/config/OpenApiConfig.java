package co.edu.univalle.patentsweep.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Contrato OpenAPI (P24) con esquema de seguridad Bearer para probar desde Swagger UI. */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "API Patent Sweep",
        version = "1.0.0",
        description = "Vigilancia de patentes sobre las fórmulas de cuchilla de Cutit Saws Ltd."))
@SecurityScheme(
        name = OpenApiConfig.ESQUEMA_SEGURIDAD,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token obtenido en POST /api/v1/auth/login")
public class OpenApiConfig {

    public static final String ESQUEMA_SEGURIDAD = "bearerAuth";
}
