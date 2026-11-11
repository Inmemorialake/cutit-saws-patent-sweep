package co.edu.univalle.patentsweep.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.edu.univalle.patentsweep.config.JacksonConfig;
import co.edu.univalle.patentsweep.exception.IntegracionExternaException;
import co.edu.univalle.patentsweep.exception.ServicioNoDisponibleException;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FuentePatentesClientTest {

    private static final String RUTA = "http://patentes.test/patentes/buscar";

    private MockRestServiceServer servidor;
    private FuentePatentesClient cliente;

    @BeforeEach
    void preparar() {
        // Mismo formato de fechas que la aplicación (JacksonConfig): dd/MM/yyyy
        Jackson2ObjectMapperBuilder jackson = Jackson2ObjectMapperBuilder.json();
        new JacksonConfig().formatoFechas().customize(jackson);
        RestClient.Builder constructor = RestClient.builder().baseUrl("http://patentes.test")
                .messageConverters(conversores -> conversores.add(0,
                        new MappingJackson2HttpMessageConverter(jackson.build())));
        servidor = MockRestServiceServer.bindTo(constructor).build();
        cliente = new FuentePatentesClient(constructor.build());
    }

    @Test
    @DisplayName("Envía los filtros como parámetros y traduce la lista de patentes")
    void deberiaBuscarPatentesConLosFiltros() {
        // Arrange
        String json = """
                [{"patenteId":"PAT-100001","tituloPatente":"Aleación de hierro y cobalto",
                  "estadoPatente":"CONCEDIDA","fechaSolicitud":"15/03/2025","fechaConcesion":"20/01/2026",
                  "campoDesconocido":"se ignora","reivindicaciones":["R1"],"clasificaciones":["C22C"]}]""";
        servidor.expect(requestToUriTemplate(RUTA + "?terminos={t}&clasificaciones={c}&fechaSolicitudDesde={f}",
                        "Hierro,Cobalto", "C22C,B24D", "2025-01-01"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        // Act
        List<PatenteExternaDTO> patentes = cliente.buscarPatentes(List.of("Hierro", "Cobalto"),
                List.of("C22C", "B24D"), LocalDate.of(2025, 1, 1));

        // Assert
        assertThat(patentes).hasSize(1);
        assertThat(patentes.get(0).patenteId()).isEqualTo("PAT-100001");
        assertThat(patentes.get(0).estadoPatente()).isEqualTo(EstadoVigenciaPatente.CONCEDIDA);
        assertThat(patentes.get(0).fechaConcesion()).isEqualTo(LocalDate.of(2026, 1, 20));
        servidor.verify();
    }

    @Test
    @DisplayName("Sin clasificaciones ni fecha solo envía los términos")
    void deberiaOmitirLosFiltrosVacios() {
        // Arrange
        servidor.expect(queryParam("terminos", "Hierro"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        // Act
        List<PatenteExternaDTO> patentes = cliente.buscarPatentes(List.of("Hierro"), List.of(), null);

        // Assert
        assertThat(patentes).isEmpty();
        servidor.verify();
    }

    @Test
    @DisplayName("Sin ningún filtro consulta la ruta sin parámetros")
    void deberiaConsultarSinParametros() {
        // Arrange
        servidor.expect(requestToUriTemplate(RUTA))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        // Act
        List<PatenteExternaDTO> patentes = cliente.buscarPatentes(null, null, null);

        // Assert
        assertThat(patentes).isEmpty();
        servidor.verify();
    }

    @Test
    @DisplayName("Un 4xx se traduce a un error de integración que no se reintenta")
    void deberiaTraducirLosErrores4xx() {
        // Arrange
        servidor.expect(requestToUriTemplate(RUTA + "?terminos={t}", "Hierro")).andRespond(withBadRequest());

        // Act / Assert
        assertThatThrownBy(() -> cliente.buscarPatentes(List.of("Hierro"), null, null))
                .isInstanceOf(IntegracionExternaException.class);
    }

    @Test
    @DisplayName("Un 5xx se traduce a servicio no disponible (reintentable)")
    void deberiaTraducirElErrorDelServidor() {
        // Arrange
        servidor.expect(requestToUriTemplate(RUTA + "?terminos={t}", "FALLO-SIMULADO"))
                .andRespond(withServerError());

        // Act / Assert
        assertThatThrownBy(() -> cliente.buscarPatentes(List.of("FALLO-SIMULADO"), null, null))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("No se pudo comunicar con la fuente de patentes");
    }

    @Test
    @DisplayName("Un fallo de red o timeout se traduce a servicio no disponible")
    void deberiaTraducirUnFalloDeRed() {
        // Arrange
        servidor.expect(requestToUriTemplate(RUTA)).andRespond(solicitud -> {
            throw new IOException("Read timed out");
        });

        // Act / Assert
        assertThatThrownBy(() -> cliente.buscarPatentes(null, null, null))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("Una respuesta vacía se trata como servicio no disponible")
    void deberiaTratarUnaRespuestaVaciaComoServicioNoDisponible() {
        // Arrange
        servidor.expect(requestToUriTemplate(RUTA))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        // Act / Assert
        assertThatThrownBy(() -> cliente.buscarPatentes(null, null, null))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("Un contenido ilegible se traduce a error de integración")
    void deberiaTraducirUnContenidoIlegible() {
        // Arrange
        servidor.expect(requestToUriTemplate(RUTA))
                .andRespond(withSuccess("esto no es json", MediaType.APPLICATION_JSON));

        // Act / Assert
        assertThatThrownBy(() -> cliente.buscarPatentes(null, null, null))
                .isInstanceOf(IntegracionExternaException.class);
    }
}
