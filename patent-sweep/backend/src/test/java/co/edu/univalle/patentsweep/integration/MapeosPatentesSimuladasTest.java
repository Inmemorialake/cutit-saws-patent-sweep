package co.edu.univalle.patentsweep.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import co.edu.univalle.patentsweep.config.JacksonConfig;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Los mapeos de la API simulada de patentes (mocks/mappings) deben poder leerse con el mismo Jackson que usa
 * la aplicación: fechas dd/MM/yyyy y estados válidos. Se omite si la carpeta de mapeos no está disponible.
 */
class MapeosPatentesSimuladasTest {

    private static final Path CARPETA = Path.of("..", "mocks", "mappings");

    @Test
    @DisplayName("Las respuestas de patentes del simulador se leen con la configuración real de Jackson")
    void deberiaLeerLasRespuestasDelSimulador() throws IOException {
        // Arrange
        assumeTrue(Files.isDirectory(CARPETA), "No se encontró la carpeta mocks/mappings");
        Jackson2ObjectMapperBuilder jackson = Jackson2ObjectMapperBuilder.json();
        new JacksonConfig().formatoFechas().customize(jackson);
        ObjectMapper mapeador = jackson.build();

        // Act
        int leidas = 0;
        try (Stream<Path> archivos = Files.list(CARPETA)) {
            for (Path archivo : archivos.filter(a -> a.getFileName().toString().startsWith("patentes-form-")).toList()) {
                JsonNode cuerpo = mapeador.readTree(archivo.toFile()).path("response").path("jsonBody");
                List<PatenteExternaDTO> patentes = mapeador.readValue(cuerpo.traverse(),
                        new TypeReference<List<PatenteExternaDTO>>() { });

                // Assert
                assertThat(patentes).allSatisfy(patente -> {
                    assertThat(patente.patenteId()).matches("PAT-\\d{6}");
                    assertThat(patente.estadoPatente()).isNotNull();
                    assertThat(patente.fechaSolicitud()).isNotNull();
                });
                leidas += patentes.size();
            }
        }
        assertThat(leidas).isPositive();
    }
}
