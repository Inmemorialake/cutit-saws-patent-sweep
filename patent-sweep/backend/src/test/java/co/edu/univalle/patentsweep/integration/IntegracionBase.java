package co.edu.univalle.patentsweep.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import co.edu.univalle.patentsweep.config.JwtProvider;
import co.edu.univalle.patentsweep.model.enums.Rol;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de las pruebas de integración de HU-02: arranca el contexto completo de Spring contra un
 * PostgreSQL real (Testcontainers, con las migraciones Flyway) y un WireMock que hace de
 * Materiales/Fórmulas y de la fuente externa de patentes.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegracionBase {

    protected static final String RUTA = "/api/v1/patentes/busqueda";
    protected static final String USUARIO = "ingeniera";

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final WireMockServer WIREMOCK = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        POSTGRES.start();
        WIREMOCK.start();
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected JwtProvider jwtProvider;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.flyway.enabled", () -> "true");
        registro.add("patentsweep.materiales.url-base", WIREMOCK::baseUrl);
        registro.add("patentsweep.fuente-patentes.url-base", WIREMOCK::baseUrl);
    }

    @BeforeEach
    void prepararEscenario() {
        WIREMOCK.resetAll();
        jdbc.update("DELETE FROM conflicto");
        jdbc.update("DELETE FROM revision_formula");
        jdbc.update("INSERT INTO usuario (username, password_hash, rol) VALUES (?, 'hash-de-prueba', 'ADMINISTRADOR') "
                + "ON CONFLICT (username) DO NOTHING", USUARIO);
    }

    protected String tokenAdministrador() {
        return jwtProvider.generarToken(USUARIO, Rol.ADMINISTRADOR);
    }

    protected static void materialesResponde(String formulaId, int estadoHttp, String cuerpo) {
        WIREMOCK.stubFor(get(urlEqualTo("/formulas/" + formulaId))
                .willReturn(aResponse().withStatus(estadoHttp)
                        .withHeader("Content-Type", "application/json").withBody(cuerpo)));
    }

    protected static void fuenteResponde(int estadoHttp, String cuerpo) {
        WIREMOCK.stubFor(get(urlPathEqualTo("/patentes/buscar"))
                .willReturn(aResponse().withStatus(estadoHttp)
                        .withHeader("Content-Type", "application/json").withBody(cuerpo)));
    }

    /** form-001: 4 materiales, así que la regla exige al menos 2 mencionados en la patente. */
    protected static String formulaJson(String codigo, String estado) {
        return "{\"codigo\":\"" + codigo + "\",\"nombre\":\"Aleación Cuchilla Diamante A\","
                + "\"aplicacionTecnica\":\"Cadena de corte para motosierra Ripit 5000\","
                + "\"version\":\"1\",\"estado\":\"" + estado + "\",\"componentes\":["
                + "{\"skuMaterial\":\"mat-001\",\"nombreMaterial\":\"Hierro\",\"porcentaje\":60.0},"
                + "{\"skuMaterial\":\"mat-002\",\"nombreMaterial\":\"Cobalto\",\"porcentaje\":20.0},"
                + "{\"skuMaterial\":\"mat-003\",\"nombreMaterial\":\"Níquel\",\"porcentaje\":15.0},"
                + "{\"skuMaterial\":\"mat-004\",\"nombreMaterial\":\"Carbono\",\"porcentaje\":5.0}]}";
    }

    protected static String patenteJson(String id, String titulo, String resumen) {
        return "{\"patenteId\":\"" + id + "\",\"tituloPatente\":\"" + titulo + "\","
                + "\"tipoProducto\":\"Materiales\",\"estadoPatente\":\"CONCEDIDA\","
                + "\"fechaSolicitud\":\"15/03/2025\",\"fechaConcesion\":\"20/01/2026\","
                + "\"fechaVencimiento\":\"15/03/2045\",\"jurisdiccion\":\"US\",\"resumen\":\"" + resumen + "\","
                + "\"reivindicaciones\":[],\"clasificaciones\":[\"B27B 17/00\"]}";
    }

    /** Dos patentes que mencionan 2 o más materiales (conflicto) y una que solo menciona 1 (no conflicto). */
    protected static String patentesMixtas() {
        return "[" + patenteJson("PAT-100001", "Aleación de hierro, cobalto y níquel", "Cadenas de corte") + ","
                + patenteJson("PAT-100002", "Cuchilla reforzada con hierro y cobalto", "Cuchilla de corte") + ","
                + patenteJson("PAT-100003", "Cuchilla reforzada con cobalto", "Cuchilla de corte") + "]";
    }
}
