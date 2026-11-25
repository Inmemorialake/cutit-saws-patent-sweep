package co.edu.univalle.patentsweep.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Seguridad (P26 y P27, DA-11): sesión sin estado con JWT y control de acceso por rol
 * mediante {@code @PreAuthorize}. El health check y la documentación son públicos. Las métricas
 * ({@code /actuator/prometheus}) solo se sirven en el puerto de management, que no se publica fuera de
 * la red de Docker: ahí Prometheus las lee sin credenciales; en cualquier otro puerto exigen JWT
 * (ADR-008, ADR-010). CSRF se desactiva porque el API es sin estado y el JWT viaja en la cabecera,
 * sin cookies de sesión.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
        "/api/v1/auth/login",
        "/actuator/health",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    private static final String RUTA_METRICAS = "/actuator/prometheus";

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                               ObjectMapper objectMapper,
                                               @Value("${management.server.port:-1}") int puertoManagement)
            throws Exception {
        // Solo por el puerto interno de management: si alguien publicara ese puerto por error o
        // igualara los puertos, las métricas seguirían pidiendo JWT en el puerto público.
        RequestMatcher metricasInternas = peticion -> puertoManagement > 0
                && peticion.getLocalPort() == puertoManagement
                && RUTA_METRICAS.equals(peticion.getRequestURI());
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(metricasInternas).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((peticion, respuesta, ex) -> escribir(respuesta, objectMapper,
                                HttpStatus.UNAUTHORIZED, "No autenticado", "Debe iniciar sesión para continuar"))
                        .accessDeniedHandler((peticion, respuesta, ex) -> escribir(respuesta, objectMapper,
                                HttpStatus.FORBIDDEN, "Acceso denegado",
                                "No tiene permisos para realizar esta operación")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtAuthFilter filtroJwt(JwtProvider jwtProvider) {
        return new JwtAuthFilter(jwtProvider);
    }

    @Bean
    public PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager gestorDeAutenticacion(AuthenticationConfiguration configuracion)
            throws Exception {
        return configuracion.getAuthenticationManager();
    }

    private static void escribir(HttpServletResponse respuesta, ObjectMapper objectMapper, HttpStatus estado,
                                 String titulo, String detalle) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setProperty("traceId", MDC.get("traceId"));
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(respuesta.getOutputStream(), problema);
    }
}
