package co.edu.univalle.patentsweep.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.univalle.patentsweep.model.enums.Rol;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-02, criterios 2 a 6: flujo completo con BD real y APIs externas simuladas. */
class BusquedaManualIntegracionTest extends IntegracionBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("200: registra solo las patentes que cumplen la regla y deja historial con el usuario del token")
    void deberiaRegistrarConflictosEHistorial() throws Exception {
        materialesResponde("form-001", 200, formulaJson("form-001", "APROBADA"));
        fuenteResponde(200, patentesMixtas());

        buscar("form-001", tokenAdministrador())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formulaId").value("form-001"))
                .andExpect(jsonPath("$.conflictos.length()").value(2))
                .andExpect(jsonPath("$.conflictos[0].conflictoId").value(matchesPattern("\\d{6}")))
                .andExpect(jsonPath("$.conflictos[0].estadoPatente").value("CONCEDIDA"))
                .andExpect(jsonPath("$.conflictos[0].fechaConcesion").value("20/01/2026"));

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM conflicto WHERE formula_id = 'form-001' AND estado = 'DETECTADO'",
                Integer.class)).isEqualTo(2);
        Map<String, Object> revision = jdbc.queryForMap(
                "SELECT r.origen, r.resultado, u.username FROM revision_formula r "
                        + "JOIN usuario u ON u.usuario_id = r.usuario_id WHERE r.formula_id = 'form-001'");
        assertThat(revision).containsEntry("origen", "BUSQUEDA_MANUAL")
                .containsEntry("resultado", "CON_CONFLICTO")
                .containsEntry("username", USUARIO);
    }

    @Test
    @DisplayName("200: repetir la búsqueda no duplica conflictos por pareja fórmula-patente")
    void noDeberiaDuplicarConflictos() throws Exception {
        materialesResponde("form-001", 200, formulaJson("form-001", "APROBADA"));
        fuenteResponde(200, patentesMixtas());

        buscar("form-001", tokenAdministrador()).andExpect(status().isOk());
        buscar("form-001", tokenAdministrador())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictos.length()").value(2));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM conflicto", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM revision_formula", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("200: sin coincidencias devuelve lista vacía, el mensaje del PB y SIN_CONFLICTO")
    void deberiaResponderListaVaciaSinCoincidencias() throws Exception {
        materialesResponde("form-001", 200, formulaJson("form-001", "APROBADA"));
        fuenteResponde(200, "[" + patenteJson("PAT-100003", "Cuchilla reforzada con cobalto", "Corte") + "]");

        buscar("form-001", tokenAdministrador())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictos").isEmpty())
                .andExpect(jsonPath("$.mensaje").value("No se detectaron conflictos relacionados con su búsqueda"));

        assertThat(jdbc.queryForObject("SELECT resultado FROM revision_formula", String.class))
                .isEqualTo("SIN_CONFLICTO");
    }

    @Test
    @DisplayName("400: formulaId sin el formato form-NNN y sin tocar las APIs externas")
    void deberiaResponder400ConFormatoInvalido() throws Exception {
        buscar("FORM-1", tokenAdministrador())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("El código de la fórmula es obligatorio y debe tener el formato form-NNN"));

        assertThat(WIREMOCK.getAllServeEvents()).isEmpty();
    }

    @Test
    @DisplayName("404: la fórmula no existe en Materiales y no se registra historial")
    void deberiaResponder404SiLaFormulaNoExiste() throws Exception {
        materialesResponde("form-404", 404, "{\"mensaje\":\"no existe\"}");

        buscar("form-404", tokenAdministrador())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("La fórmula consultada no fue encontrada"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM revision_formula", Integer.class)).isZero();
    }

    @Test
    @DisplayName("409: la fórmula DESCONTINUADA no consulta la fuente de patentes")
    void deberiaResponder409SiLaFormulaEstaDescontinuada() throws Exception {
        materialesResponde("form-003", 200, formulaJson("form-003", "DESCONTINUADA"));

        buscar("form-003", tokenAdministrador())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("No se puede buscar patentes para una fórmula DESCONTINUADA"));

        WIREMOCK.verify(0, getRequestedFor(urlPathEqualTo("/patentes/buscar")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM revision_formula", Integer.class)).isZero();
    }

    @Test
    @DisplayName("401 sin token y 403 con rol SERVICIO")
    void deberiaExigirTokenYRol() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"formulaId\":\"form-001\"}"))
                .andExpect(status().isUnauthorized());

        buscar("form-001", jwtProvider.generarToken("servicio", Rol.SERVICIO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BD: la unicidad del conflicto es por fórmula + patente, sin la versión (V8)")
    void deberiaTenerUnicidadPorFormulaYPatente() {
        List<String> columnas = jdbc.queryForList(
                "SELECT a.attname FROM pg_constraint c JOIN pg_attribute a "
                        + "ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey) "
                        + "WHERE c.conname = 'uq_conflicto_formula_patente' ORDER BY a.attname", String.class);

        assertThat(columnas).containsExactly("formula_id", "patente_id");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conname = 'uq_conflicto_formula_patente_version'",
                Integer.class)).isZero();
    }

    private ResultActions buscar(String formulaId, String token) throws Exception {
        return mockMvc.perform(post(RUTA)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formulaId\":\"" + formulaId + "\"}"));
    }
}
