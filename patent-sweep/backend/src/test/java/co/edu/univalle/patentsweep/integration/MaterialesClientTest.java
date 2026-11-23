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
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MaterialesClientTest {

    private static final String URL = "http://materiales.test/formulas/form-001";

    private MockRestServiceServer servidor;
    private MaterialesClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder().baseUrl("http://materiales.test");
        servidor = MockRestServiceServer.bindTo(constructor).build();
        cliente = new MaterialesClient(constructor.build());
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
}
