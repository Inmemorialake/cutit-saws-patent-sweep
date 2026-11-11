package co.edu.univalle.patentsweep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.univalle.patentsweep.exception.FormulaNoEncontradaException;
import co.edu.univalle.patentsweep.integration.FuentePatentesClient;
import co.edu.univalle.patentsweep.integration.MaterialesClient;
import co.edu.univalle.patentsweep.integration.NotificacionesClient;
import co.edu.univalle.patentsweep.integration.dto.ComponenteExternoDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.model.Notificacion;
import co.edu.univalle.patentsweep.model.enums.EstadoConflicto;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import co.edu.univalle.patentsweep.model.enums.EstadoNotificacion;
import co.edu.univalle.patentsweep.repository.ConflictoRepository;
import co.edu.univalle.patentsweep.repository.NotificacionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Flujo completo de HU-02 contra una base PostgreSQL real y embebida: migraciones Flyway, validación del
 * mapeo JPA, login con JWT, búsqueda, historial, estado de la fórmula y notificación asíncrona.
 * Solo se simulan las APIs externas (Materiales, Fuente de patentes y Notificaciones).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "patentsweep.seguridad.admin-usuario=admin",
    "patentsweep.seguridad.admin-clave=clave-de-prueba-123"
})
class FlujoBusquedaIntegracionTest {

    private static final EmbeddedPostgres POSTGRES = iniciarPostgres();

    @DynamicPropertySource
    static void propiedadesDeLaBase(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ConflictoRepository conflictoRepository;
    @Autowired
    private NotificacionRepository notificacionRepository;

    @MockitoBean
    private MaterialesClient materialesClient;
    @MockitoBean
    private FuentePatentesClient fuentePatentesClient;
    @MockitoBean
    private NotificacionesClient notificacionesClient;

    @Test
    @Order(1)
    @DisplayName("Login, búsqueda con conflicto, idempotencia y notificación asíncrona sobre PostgreSQL real")
    void deberiaRecorrerElFlujoCompletoDeLaBusqueda() throws Exception {
        // Arrange
        when(materialesClient.obtenerFormula("form-001")).thenReturn(new FormulaExternaDTO("form-001",
                "Aleación Cuchilla Diamante A", "Cadena de corte para motosierra Ripit 5000", "1", "APROBADA",
                List.of(componente("Hierro"), componente("Cobalto"), componente("Níquel"), componente("Carbono"))));
        when(fuentePatentesClient.buscarPatentes(any(), any(), any())).thenReturn(List.of(new PatenteExternaDTO(
                "PAT-100001", "Aleación para cadenas de corte", "Materiales", EstadoVigenciaPatente.CONCEDIDA,
                LocalDate.of(2025, 1, 10), LocalDate.of(2026, 6, 22), LocalDate.of(2031, 1, 10), "US",
                "Aleación de Hierro, Cobalto y Níquel para cadenas de corte.",
                List.of("Reivindicación 1: Composición con Hierro, Cobalto y Níquel."), List.of("B27B 17/00"))));
        String token = iniciarSesion();

        // Act / Assert: primera búsqueda detecta el conflicto con id de 6 dígitos
        mockMvc.perform(buscar("form-001", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictos.length()").value(1))
                .andExpect(jsonPath("$.conflictos[0].conflictoId").value("100000"))
                .andExpect(jsonPath("$.conflictos[0].tituloPatente").value("Aleación para cadenas de corte"))
                .andExpect(jsonPath("$.conflictos[0].estadoPatente").value("CONCEDIDA"))
                .andExpect(jsonPath("$.conflictos[0].fechaConcesion").value("22/06/2026"));

        // Assert: la fórmula quedó bloqueada y el conflicto es único
        mockMvc.perform(get("/api/v1/patentes/estado").param("formulaId", "form-001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFLICTO_DETECTADO"));
        assertThat(conflictoRepository.count()).isEqualTo(1);

        // Assert: segunda búsqueda no duplica el conflicto (PB HU-01, 6.2)
        mockMvc.perform(buscar("form-001", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictos[0].conflictoId").value("100000"));
        assertThat(conflictoRepository.count()).isEqualTo(1);

        // Assert: la notificación se envía una sola vez, en segundo plano, y queda NOTIFICADO
        verify(notificacionesClient, timeout(10_000)).enviar(any());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(notificacionRepository.findAll()).extracting(Notificacion::getEstado)
                        .containsExactly(EstadoNotificacion.NOTIFICADO));
    }

    @Test
    @Order(2)
    @DisplayName("Una versión nueva de la fórmula genera un conflicto nuevo con la misma patente (DA-17)")
    void deberiaGenerarUnConflictoNuevoPorCadaVersionDeLaFormula() throws Exception {
        // Arrange
        when(materialesClient.obtenerFormula("form-020")).thenReturn(formulaConVersion("1"), formulaConVersion("1"),
                formulaConVersion("2"));
        when(fuentePatentesClient.buscarPatentes(any(), any(), any())).thenReturn(List.of(new PatenteExternaDTO(
                "PAT-100001", "Aleación para cadenas de corte", "Materiales", EstadoVigenciaPatente.CONCEDIDA,
                LocalDate.of(2025, 1, 10), LocalDate.of(2026, 6, 22), LocalDate.of(2031, 1, 10), "US",
                "Aleación de Hierro, Cobalto y Níquel para cadenas de corte.",
                List.of("Reivindicación 1: Composición con Hierro, Cobalto y Níquel."), List.of("B27B 17/00"))));
        String token = iniciarSesion();

        // Act: dos búsquedas con la versión 1 y una con la versión 2
        mockMvc.perform(buscar("form-020", token)).andExpect(status().isOk());
        mockMvc.perform(buscar("form-020", token)).andExpect(status().isOk());
        mockMvc.perform(buscar("form-020", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictos.length()").value(1));

        // Assert: un conflicto por versión, sin duplicados dentro de la misma versión
        assertThat(conflictoRepository.countByFormulaIdAndVersionFormulaAndEstado("form-020", "1",
                EstadoConflicto.DETECTADO)).isEqualTo(1);
        assertThat(conflictoRepository.countByFormulaIdAndVersionFormulaAndEstado("form-020", "2",
                EstadoConflicto.DETECTADO)).isEqualTo(1);

        // Assert: cada revisión guarda la versión y la foto de la fórmula evaluada
        assertThat(jdbcTemplate.queryForList("SELECT version_formula FROM revision_formula "
                + "WHERE formula_id = 'form-020' ORDER BY revision_id", String.class))
                .containsExactly("1", "1", "2");
        assertThat(jdbcTemplate.queryForObject("SELECT parametros->'formula'->'componentes'->0->>'nombreMaterial' "
                + "FROM revision_formula WHERE formula_id = 'form-020' ORDER BY revision_id LIMIT 1", String.class))
                .isEqualTo("Hierro");

        // Assert: una versión que nadie ha revisado deja la fórmula en NO_PROCESADO
        when(materialesClient.obtenerFormula("form-020")).thenReturn(formulaConVersion("3"));
        mockMvc.perform(get("/api/v1/patentes/estado").param("formulaId", "form-020")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("NO_PROCESADO"));
    }

    private static FormulaExternaDTO formulaConVersion(String version) {
        return new FormulaExternaDTO("form-020", "Aleación con versión", "Cadena de corte", version, "APROBADA",
                List.of(componente("Hierro"), componente("Cobalto"), componente("Níquel"), componente("Carbono")));
    }

    @Test
    @Order(3)
    @DisplayName("Sin token la API responde 401 y con una fórmula inexistente o descontinuada responde 404 y 409")
    void deberiaAplicarSeguridadYErroresDeNegocio() throws Exception {
        // Arrange
        when(materialesClient.obtenerFormula("form-404")).thenThrow(new FormulaNoEncontradaException());
        when(materialesClient.obtenerFormula("form-003")).thenReturn(new FormulaExternaDTO("form-003", "Antigua",
                "Uso", "1", "DESCONTINUADA", List.of()));
        String token = iniciarSesion();

        // Act / Assert
        mockMvc.perform(post("/api/v1/patentes/busqueda").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formulaId\":\"form-001\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(buscar("form-404", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("La fórmula consultada no fue encontrada"));
        mockMvc.perform(buscar("form-003", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede buscar patentes para una fórmula DESCONTINUADA"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"clave\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales inválidas"));
    }

    private String iniciarSesion() throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"clave\":\"clave-de-prueba-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("token").asText();
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder buscar(
            String formulaId, String token) {
        return post("/api/v1/patentes/busqueda")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formulaId\":\"" + formulaId + "\"}");
    }

    private static ComponenteExternoDTO componente(String nombre) {
        return new ComponenteExternoDTO("SKU-" + nombre, nombre, BigDecimal.TEN);
    }

    private static EmbeddedPostgres iniciarPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo iniciar PostgreSQL embebido", e);
        }
    }
}
