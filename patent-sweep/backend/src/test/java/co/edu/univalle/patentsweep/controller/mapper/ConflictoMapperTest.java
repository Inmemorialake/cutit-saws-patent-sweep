package co.edu.univalle.patentsweep.controller.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.patentsweep.controller.dto.BusquedaResponseDTO;
import co.edu.univalle.patentsweep.controller.dto.ConflictoResumenDTO;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConflictoMapperTest {

    private final ConflictoMapper mapper = new ConflictoMapper();

    @Test
    @DisplayName("El identificador se completa a 6 dígitos y el título se corta a 60 caracteres")
    void deberiaFormatearElIdentificadorYCortarElTitulo() {
        // Arrange
        String tituloLargo = "T".repeat(80);
        Conflicto conflicto = conflicto(100001L, tituloLargo);

        // Act
        ConflictoResumenDTO resumen = mapper.aResumen(conflicto);

        // Assert
        assertThat(resumen.conflictoId()).isEqualTo("100001");
        assertThat(resumen.tituloPatente()).hasSize(60);
        assertThat(resumen.estadoPatente()).isEqualTo(EstadoVigenciaPatente.CONCEDIDA);
    }

    @Test
    @DisplayName("Un título de hasta 60 caracteres no se modifica")
    void noDeberiaCortarUnTituloCorto() {
        // Arrange
        Conflicto conflicto = conflicto(100002L, "Aleación para cadenas de corte");

        // Act
        ConflictoResumenDTO resumen = mapper.aResumen(conflicto);

        // Assert
        assertThat(resumen.tituloPatente()).isEqualTo("Aleación para cadenas de corte");
    }

    @Test
    @DisplayName("La respuesta con conflictos lleva el mensaje de éxito")
    void deberiaArmarLaRespuestaConConflictos() {
        // Act
        BusquedaResponseDTO respuesta = mapper.aRespuestaDeBusqueda("form-001", List.of(conflicto(100001L, "Título")));

        // Assert
        assertThat(respuesta.formulaId()).isEqualTo("form-001");
        assertThat(respuesta.conflictos()).hasSize(1);
        assertThat(respuesta.mensaje()).isEqualTo("Búsqueda completada correctamente");
    }

    @Test
    @DisplayName("La respuesta sin conflictos lleva el mensaje del PB")
    void deberiaArmarLaRespuestaSinConflictos() {
        // Act
        BusquedaResponseDTO respuesta = mapper.aRespuestaDeBusqueda("form-002", List.of());

        // Assert
        assertThat(respuesta.conflictos()).isEmpty();
        assertThat(respuesta.mensaje()).isEqualTo("No se detectaron conflictos relacionados con su búsqueda");
    }

    private static Conflicto conflicto(Long id, String titulo) {
        Patente patente = Patente.builder().titulo(titulo).tipoProducto("Aleación")
                .estadoVigencia(EstadoVigenciaPatente.CONCEDIDA).build();
        return Conflicto.builder().conflictoId(id).patente(patente).build();
    }
}
