package co.edu.univalle.patentsweep.integration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.dto.NotificacionRequestDTO;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NotificacionesClientTest {

    private static final String URL = "http://notificaciones.test/api/v1/notificaciones";
    private static final NotificacionRequestDTO ALERTA = new NotificacionRequestDTO("100001", "form-001",
            "PAT-998822", "CONFLICTO_PATENTE_DETECTADO", "Se ha identificado un conflicto de patente en la fórmula form-001.");

    private MockRestServiceServer servidor;
    private NotificacionesClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder().baseUrl("http://notificaciones.test");
        servidor = MockRestServiceServer.bindTo(constructor).build();
        cliente = new NotificacionesClient(constructor.build());
    }

    @Test
    @DisplayName("Envía el cuerpo del contrato del PB y acepta 201 como éxito")
    void deberiaEnviarLaAlerta() {
        // Arrange
        servidor.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.conflictoId").value("100001"))
                .andExpect(jsonPath("$.formulaId").value("form-001"))
                .andExpect(jsonPath("$.patenteId").value("PAT-998822"))
                .andExpect(jsonPath("$.tipoAlerta").value("CONFLICTO_PATENTE_DETECTADO"))
                .andRespond(withStatus(HttpStatus.CREATED));

        // Act / Assert
        assertThatCode(() -> cliente.enviar(ALERTA)).doesNotThrowAnyException();
        servidor.verify();
    }

    @Test
    @DisplayName("Acepta 200 como éxito")
    void deberiaAceptarUn200() {
        // Arrange
        servidor.expect(requestTo(URL)).andRespond(withSuccess());

        // Act / Assert
        assertThatCode(() -> cliente.enviar(ALERTA)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un 4xx no es reintentable y se traduce a error de integración")
    void deberiaTraducirUn4xx() {
        // Arrange
        servidor.expect(requestTo(URL)).andRespond(withBadRequest());

        // Act / Assert
        assertThatThrownBy(() -> cliente.enviar(ALERTA)).isInstanceOf(IntegracionExternaException.class);
    }

    @Test
    @DisplayName("Un 5xx se traduce a servicio no disponible (reintentable)")
    void deberiaTraducirUn5xx() {
        // Arrange
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        // Act / Assert
        assertThatThrownBy(() -> cliente.enviar(ALERTA))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("No se pudo comunicar con el servicio de Notificaciones");
    }

    @Test
    @DisplayName("Un timeout se traduce a servicio no disponible")
    void deberiaTraducirUnTimeout() {
        // Arrange
        servidor.expect(requestTo(URL)).andRespond(solicitud -> {
            throw new IOException("Read timed out");
        });

        // Act / Assert
        assertThatThrownBy(() -> cliente.enviar(ALERTA)).isInstanceOf(ServicioNoDisponibleException.class);
    }
}
