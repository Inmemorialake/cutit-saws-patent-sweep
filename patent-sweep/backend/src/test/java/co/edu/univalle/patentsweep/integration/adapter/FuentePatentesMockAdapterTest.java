package co.edu.univalle.patentsweep.integration.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import java.time.LocalDate;
import java.util.List;

import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FuentePatentesMockAdapterTest {

    private FuentePatentesMockAdapter adaptador;

    @BeforeEach
    void preparar() {
        adaptador = new FuentePatentesMockAdapter();
    }

    @Test
    @DisplayName("Sin filtros devuelve todo el dataset preconfigurado")
    void deberiaDevolverTodoElDatasetSinFiltros() {
        // Act
        List<PatenteExternaDTO> resultado = adaptador.buscarPatentes(null, null, null);

        // Assert
        assertThat(resultado).extracting(PatenteExternaDTO::patenteId)
                .containsExactly("PAT-2026-999", "PAT-2025-888", "PAT-2024-111");
        assertThat(resultado.get(0).fechaSolicitud()).isEqualTo(LocalDate.of(2025, 3, 15));
    }

    @Test
    @DisplayName("Filtra por clasificación IPC/CPC")
    void deberiaFiltrarPorClasificacion() {
        // Act
        List<PatenteExternaDTO> resultado = adaptador.buscarPatentes(List.of(), List.of("B24D 3/00"), null);

        // Assert
        assertThat(resultado).extracting(PatenteExternaDTO::patenteId)
                .containsExactly("PAT-2026-999", "PAT-2025-888");
    }

    @Test
    @DisplayName("Excluye las patentes solicitadas antes de la fecha mínima")
    void deberiaFiltrarPorFechaDeSolicitud() {
        // Act
        List<PatenteExternaDTO> resultado = adaptador.buscarPatentes(List.of(), List.of(), LocalDate.of(2025, 1, 1));

        // Assert
        assertThat(resultado).extracting(PatenteExternaDTO::patenteId)
                .containsExactly("PAT-2026-999");
    }

    @Test
    @DisplayName("Filtra por términos sin distinguir mayúsculas")
    void deberiaFiltrarPorTerminos() {
        // Act
        List<PatenteExternaDTO> resultado = adaptador.buscarPatentes(List.of("CROMO"), List.of(), null);

        // Assert
        assertThat(resultado).extracting(PatenteExternaDTO::patenteId)
                .containsExactly("PAT-2025-888");
    }

    @Test
    @DisplayName("Escenario de fallo técnico: lanza RuntimeException al detectar término de error")
    void deberiaLanzarExcepcionEnFalloTecnico() {
        // Act & Assert
        assertThatThrownBy(() -> adaptador.buscarPatentes(List.of("error-tecnico"), List.of(), LocalDate.now()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falla simulada en la red de la fuente externa");
    }

    @Test
    @DisplayName("Escenario de fórmula limpia: retorna lista vacía")
    void deberiaRetornarListaVaciaEnFormulaLimpia() {
        // Act
        List<PatenteExternaDTO> resultado = adaptador.buscarPatentes(List.of("formula-limpia"), List.of(), null);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Camino feliz: valida los datos exactos y completos de las patentes candidatas preconfiguradas")
    void deberiaRetornarCaminoFelizConDatosCompletos() {
        // Act
        List<PatenteExternaDTO> resultado = adaptador.buscarPatentes(List.of("cuchilla"), List.of("B23D"), null);

        // Assert
        assertThat(resultado).isNotEmpty();
        assertThat(resultado.get(0).patenteId()).isEqualTo("PAT-2026-999");
        assertThat(resultado.get(0).tituloPatente()).contains("Cuchilla hidráulica de diamante");
        assertThat(resultado.get(0).estadoPatente()).isEqualTo(EstadoVigenciaPatente.CONCEDIDA);
        assertThat(resultado.get(0).clasificaciones()).contains("B23D");
    }
}