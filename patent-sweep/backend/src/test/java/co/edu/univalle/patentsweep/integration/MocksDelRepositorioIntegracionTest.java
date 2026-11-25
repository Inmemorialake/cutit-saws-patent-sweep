package co.edu.univalle.patentsweep.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.univalle.patentsweep.config.JwtProvider;
import co.edu.univalle.patentsweep.model.Barrido;
import co.edu.univalle.patentsweep.model.enums.EstadoBarrido;
import co.edu.univalle.patentsweep.model.enums.EstadoFormula;
import co.edu.univalle.patentsweep.model.enums.Rol;
import co.edu.univalle.patentsweep.service.BarridoService;
import co.edu.univalle.patentsweep.service.EstadoFormulaService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Comprueba que los mocks de {@code patent-sweep/mocks/mappings} (los mismos que usan Docker Compose y
 * producción) permiten recorrer HU-01, HU-02 y HU-04 de punta a punta. Si alguien rompe o borra un mock, el
 * CI falla aquí en lugar de descubrirlo en la demo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MocksDelRepositorioIntegracionTest {

    private static final Path MOCKS = Path.of(System.getProperty("user.dir"), "..", "mocks").normalize();
    private static final String USUARIO = "demo";

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final WireMockServer WIREMOCK = new WireMockServer(WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderDirectory(MOCKS.toString()));

    static {
        POSTGRES.start();
        WIREMOCK.start();
    }

    @Autowired
    private BarridoService barridoService;
    @Autowired
    private EstadoFormulaService estadoFormulaService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private JdbcTemplate jdbc;

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
    void limpiar() {
        assertThat(Files.isDirectory(MOCKS.resolve("mappings"))).as("carpeta de mocks %s", MOCKS).isTrue();
        jdbc.update("DELETE FROM conflicto");
        jdbc.update("DELETE FROM revision_formula");
        jdbc.update("DELETE FROM barrido");
        jdbc.update("INSERT INTO usuario (username, password_hash, rol) VALUES (?, 'hash-de-prueba', 'ADMINISTRADOR') "
                + "ON CONFLICT (username) DO NOTHING", USUARIO);
    }

    @Test
    @DisplayName("HU-01 + HU-04: el barrido con los mocks termina COMPLETADO y deja form-001/002 en conflicto y form-004 LIBRE")
    void elBarridoConLosMocksTerminaCompletado() {
        Barrido barrido = barridoService.ejecutar();

        assertThat(barrido.getEstado()).isEqualTo(EstadoBarrido.COMPLETADO);
        assertThat(barrido.getMensaje()).isEqualTo("Barrido completado exitosamente");
        assertThat(barrido.getTotalFormulas()).isEqualTo(3);
        assertThat(barrido.getTotalProcesadas()).isEqualTo(3);
        assertThat(barrido.getTotalConConflicto()).isEqualTo(2);
        assertThat(barrido.getTotalSinConflicto()).isEqualTo(1);
        assertThat(barrido.getNoProcesadas()).isZero();

        assertThat(estadoFormulaService.consultarEstado("form-001").estado())
                .isEqualTo(EstadoFormula.CONFLICTO_DETECTADO);
        assertThat(estadoFormulaService.consultarEstado("form-002").estado())
                .isEqualTo(EstadoFormula.CONFLICTO_DETECTADO);
        assertThat(estadoFormulaService.consultarEstado("form-004").estado()).isEqualTo(EstadoFormula.LIBRE);
        assertThat(estadoFormulaService.consultarEstado("form-003").estado()).isEqualTo(EstadoFormula.NO_PROCESADO);
    }

    @Test
    @DisplayName("HU-02: la búsqueda manual con los mocks cubre conflicto, sin conflicto, descontinuada e inexistente")
    void laBusquedaManualConLosMocksCubreTodosLosCasos() throws Exception {
        buscar("form-001").andExpect(status().isOk()).andExpect(jsonPath("$.conflictos.length()").value(2));
        buscar("form-002").andExpect(status().isOk()).andExpect(jsonPath("$.conflictos.length()").value(1));
        buscar("form-004").andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictos.length()").value(0))
                .andExpect(jsonPath("$.mensaje").value("No se detectaron conflictos relacionados con su búsqueda"));
        buscar("form-003").andExpect(status().isConflict());
        buscar("form-999").andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions buscar(String formulaId) throws Exception {
        return mockMvc.perform(post("/api/v1/patentes/busqueda")
                .header("Authorization", "Bearer " + jwtProvider.generarToken(USUARIO, Rol.ADMINISTRADOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formulaId\":\"" + formulaId + "\"}"));
    }
}
