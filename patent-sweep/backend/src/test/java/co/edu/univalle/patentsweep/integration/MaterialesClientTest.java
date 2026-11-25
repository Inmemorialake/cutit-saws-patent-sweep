package co.edu.univalle.patentsweep.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.edu.univalle.patentsweep.exception.FormulaNoEncontradaException;
import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaResumenDTO;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MaterialesClientTest {

    private static final String URL = "http://materiales.test/formulas/form-001";

    private MockRestServiceServer servidor;
    private MaterialesClient cliente;
    private ResilientExternalCallExecutor llamadas;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder().baseUrl("http://materiales.test");
        servidor = MockRestServiceServer.bindTo(constructor).build();
        llamadas = Mockito.mock(ResilientExternalCallExecutor.class);
        Mockito.doAnswer(invocacion -> ((Supplier<?>) invocacion.getArgument(0)).get())
                .when(llamadas).ejecutarMateriales(Mockito.any());
        Mockito.doAnswer(invocacion -> ((Supplier<?>) invocacion.getArgument(0)).get())
                .when(llamadas).ejecutarVerificacionMateriales(Mockito.any());
        cliente = new MaterialesClient(constructor.build(), llamadas);
    }

    @Test
    @DisplayName("Traduce el detalle de la fórmula con sus componentes")
    void deberiaObtenerLaFormulaConSusComponentes() {
        // Arrange
        String json = """
                {"codigo":"form-001","nombre":"Aleación Cuchilla Diamante A",
                 "aplicacionTecnica":"Cadena de corte","version":"2","estado":"APROBADA",
                 "campoDesconocido":"se ignora",
                 "componentes":[{"skuMaterial":"mat-001","nombreMaterial":"Hierro","porcentaje":60.5}]}""";
        servidor.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        // Act
        FormulaExternaDTO formula = cliente.obtenerFormula("form-001");

        // Assert
        assertThat(formula.codigo()).isEqualTo("form-001");
        assertThat(formula.estaDescontinuada()).isFalse();
        assertThat(formula.componentes()).hasSize(1);
        assertThat(formula.componentes().get(0).nombreMaterial()).isEqualTo("Hierro");
        servidor.verify();
    }

    @Test
    @DisplayName("La verificación de HU-04 usa la política de 5 s sin reintentos y lee la misma fórmula")
    void deberiaVerificarLaFormulaConLaPoliticaCorta() {
        // Arrange
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("{\"codigo\":\"form-001\",\"version\":\"3\",\"estado\":\"APROBADA\"}",
                        MediaType.APPLICATION_JSON));

        // Act
        FormulaExternaDTO formula = cliente.verificarFormula("form-001");

        // Assert
        assertThat(formula.versionRegistrada()).isEqualTo("3");
        Mockito.verify(llamadas).ejecutarVerificacionMateriales(Mockito.any());
        Mockito.verify(llamadas, Mockito.never()).ejecutarMateriales(Mockito.any());
        servidor.verify();
    }

    @Test
    @DisplayName("Una fórmula DESCONTINUADA se reconoce")
    void deberiaReconocerUnaFormulaDescontinuada() {
        // Arrange
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("{\"codigo\":\"form-001\",\"estado\":\"DESCONTINUADA\"}",
                        MediaType.APPLICATION_JSON));

        // Act
        FormulaExternaDTO formula = cliente.obtenerFormula("form-001");

        // Assert
        assertThat(formula.estaDescontinuada()).isTrue();
    }

    @Test
    @DisplayName("404 de Materiales se traduce a fórmula no encontrada")
    void deberiaTraducirElNotFound() {
        // Arrange
        servidor.expect(requestTo(URL)).andRespond(withResourceNotFound());

        // Act / Assert
        assertThatThrownBy(() -> cliente.obtenerFormula("form-001")).isInstanceOf(FormulaNoEncontradaException.class);
    }

    @Test
    @DisplayName("Otro 4xx se traduce a un error de integración que no se reintenta")
    void deberiaTraducirOtrosErrores4xx() {
        // Arrange
        servidor.expect(requestTo(URL)).andRespond(withBadRequest());

        // Act / Assert
        assertThatThrownBy(() -> cliente.obtenerFormula("form-001")).isInstanceOf(IntegracionExternaException.class);
    }

    @Test
    @DisplayName("Un 5xx se traduce a servicio no disponible (reintentable)")
    void deberiaTraducirElErrorDelServidor() {
        // Arrange
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        // Act / Assert
        assertThatThrownBy(() -> cliente.obtenerFormula("form-001"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("No se pudo comunicar con el servicio de Materiales/Fórmulas");
    }

    @Test
    @DisplayName("Un fallo de red o timeout se traduce a servicio no disponible")
    void deberiaTraducirUnFalloDeRed() {
        // Arrange
        servidor.expect(requestTo(URL)).andRespond(solicitud -> {
            throw new IOException("Read timed out");
        });

        // Act / Assert
        assertThatThrownBy(() -> cliente.obtenerFormula("form-001")).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("Una respuesta vacía se trata como servicio no disponible")
    void deberiaTratarUnaRespuestaVaciaComoServicioNoDisponible() {
        // Arrange
        servidor.expect(requestTo(URL)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        // Act / Assert
        assertThatThrownBy(() -> cliente.obtenerFormula("form-001")).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("Un contenido ilegible se traduce a error de integración")
    void deberiaTraducirUnContenidoIlegible() {
        // Arrange
        servidor.expect(requestTo(URL)).andRespond(withSuccess("esto no es json", MediaType.APPLICATION_JSON));

        // Act / Assert
        assertThatThrownBy(() -> cliente.obtenerFormula("form-001")).isInstanceOf(IntegracionExternaException.class);
    }

    @Test
    @DisplayName("Acumula páginas de 10 hasta recibir la lista vacía")
    void deberiaListarFormulasAprobadasHastaLaPaginaVacia() {
        int[] cantidades = {10, 10, 3, 0};
        for (int pagina = 1; pagina <= cantidades.length; pagina++) {
            servidor.expect(requestTo("http://materiales.test/formulas?estado=APROBADA&pagina=" + pagina))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(paginaJson(cantidades[pagina - 1], pagina), MediaType.APPLICATION_JSON));
        }

        List<FormulaResumenDTO> formulas = cliente.listarAprobadas();

        assertThat(formulas).hasSize(23);
        assertThat(formulas.get(0).codigo()).isEqualTo("form-001");
        assertThat(formulas.get(22).codigo()).isEqualTo("form-023");
        servidor.verify();
    }

    @Test
    @DisplayName("Un 400 al listar se traduce a error de integración")
    void deberiaTraducirElErrorDeListado4xx() {
        servidor.expect(requestTo("http://materiales.test/formulas?estado=APROBADA&pagina=1"))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> cliente.listarAprobadas()).isInstanceOf(IntegracionExternaException.class);
    }

    @Test
    @DisplayName("Un 500 al listar se traduce a servicio no disponible")
    void deberiaTraducirElErrorDeListado5xx() {
        servidor.expect(requestTo("http://materiales.test/formulas?estado=APROBADA&pagina=1"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> cliente.listarAprobadas())
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("No se pudo comunicar con el servicio de Materiales/Fórmulas");
    }

    @Test
    @DisplayName("La configuración define 3 intentos, 5 s de espera y 20 s de límite")
    void deberiaLeerLaConfiguracionDeResiliencia() throws IOException {
        PropertySource<?> configuracion = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml")).get(0);

        assertThat(configuracion.getProperty("resilience4j.retry.instances.materiales.max-attempts"))
                .isEqualTo(3);
        assertThat(configuracion.getProperty("resilience4j.retry.instances.materiales.wait-duration"))
                .isEqualTo("5s");
        assertThat(configuracion.getProperty("resilience4j.timelimiter.instances.materiales.timeout-duration"))
                .isEqualTo("20s");
        assertThat(configuracion.getProperty("resilience4j.timelimiter.instances.materiales.cancel-running-future"))
                .isEqualTo(true);
    }

    @Test
    @DisplayName("La verificación de HU-04 se configura con 5 s de límite y un solo intento")
    void deberiaLeerLaConfiguracionDeLaVerificacion() throws IOException {
        PropertySource<?> configuracion = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml")).get(0);

        assertThat(configuracion.getProperty("resilience4j.retry.instances.materialesVerificacion.max-attempts"))
                .isEqualTo(1);
        assertThat(configuracion
                .getProperty("resilience4j.timelimiter.instances.materialesVerificacion.timeout-duration"))
                .isEqualTo("5s");
    }

    private static String paginaJson(int cantidad, int pagina) {
        int base = (pagina - 1) * 10;
        String filas = IntStream.rangeClosed(1, cantidad)
                .mapToObj(i -> "{\"codigo\":\"form-" + String.format("%03d", base + i)
                        + "\",\"nombre\":\"Fórmula " + i + "\",\"estado\":\"APROBADA\"}")
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return "[" + filas + "]";
    }
}
