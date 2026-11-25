package co.edu.univalle.patentsweep.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-02, criterio 7: reintentos y 503 con las anotaciones de Resilience4j realmente activas. Se acorta la
 * espera entre reintentos (100 ms en lugar de 5 s) para que la prueba no tarde; la espera real de 5 s se
 * comprueba en {@code MaterialesClientTest}. Cada instancia del circuit breaker recibe solo 3 llamadas
 * fallidas aquí (mínimo para abrirse: 5), así que no interfiere.
 */
@TestPropertySource(properties = {
        "resilience4j.retry.instances.materiales.wait-duration=100ms",
        "resilience4j.retry.instances.fuentePatentes.wait-duration=100ms"
})
class BusquedaManualResilienciaTest extends IntegracionBase {

    private static final String MENSAJE_503 = "No fue posible realizar la consulta. Intente nuevamente";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("503: si la fuente falla tras 3 intentos responde 503 y registra FALLO_TECNICO")
    void deberiaRegistrarFalloTecnicoCuandoFallaLaFuente() throws Exception {
        materialesResponde("form-001", 200, formulaJson("form-001", "APROBADA"));
        fuenteResponde(503, "{\"detalle\":\"caída simulada\"}");

        buscar().andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.detail").value(MENSAJE_503));

        WIREMOCK.verify(3, getRequestedFor(urlPathEqualTo("/patentes/buscar")));
        Map<String, Object> revision = jdbc.queryForMap(
                "SELECT r.origen, r.resultado, u.username FROM revision_formula r "
                        + "JOIN usuario u ON u.usuario_id = r.usuario_id");
        assertThat(revision).containsEntry("origen", "BUSQUEDA_MANUAL")
                .containsEntry("resultado", "FALLO_TECNICO")
                .containsEntry("username", USUARIO);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conflicto", Integer.class)).isZero();
    }

    @Test
    @DisplayName("503: si Materiales falla tras 3 intentos responde 503 y NO registra historial")
    void noDeberiaRegistrarHistorialCuandoFallaMateriales() throws Exception {
        materialesResponde("form-001", 503, "{\"detalle\":\"caída simulada\"}");

        buscar().andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.detail").value(MENSAJE_503));

        WIREMOCK.verify(3, getRequestedFor(urlEqualTo("/formulas/form-001")));
        WIREMOCK.verify(0, getRequestedFor(urlPathEqualTo("/patentes/buscar")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM revision_formula", Integer.class)).isZero();
    }

    private ResultActions buscar() throws Exception {
        return mockMvc.perform(post(RUTA)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdministrador())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formulaId\":\"form-001\"}"));
    }
}
