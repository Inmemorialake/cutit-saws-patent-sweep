package co.edu.univalle.patentsweep.controller;

import co.edu.univalle.patentsweep.exception.GlobalExceptionHandler;
import co.edu.univalle.patentsweep.exception.PatenteDuplicadaException;
import co.edu.univalle.patentsweep.exception.PatenteNoEncontradaException;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.model.enums.EstadoVigenciaPatente;
import co.edu.univalle.patentsweep.service.PatenteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatenteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PatenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatenteService patenteService;

    @Test
    @DisplayName("201: registra una patente exitosamente")
    void deberiaRegistrarPatenteExitosamente() throws Exception {
        Patente patenteGuardada = Patente.builder()
                .patenteId(1L)
                .codigoPatente("PAT-123456")
                .titulo("Cuchilla láser")
                .tipoProducto("Mecanizado")
                .resumen("Resumen de prueba")
                .estadoVigencia(EstadoVigenciaPatente.EN_TRAMITE)
                .build();

        when(patenteService.registrar(any(Patente.class))).thenReturn(patenteGuardada);

        String jsonRequest = "{\"codigoPatente\":\"PAT-123456\",\"titulo\":\"Cuchilla láser\",\"tipoProducto\":\"Mecanizado\",\"resumen\":\"Resumen de prueba\"}";

        mockMvc.perform(post("/patentes/busqueda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patenteId").value(1))
                .andExpect(jsonPath("$.codigoPatente").value("PAT-123456"))
                .andExpect(jsonPath("$.titulo").value("Cuchilla láser"));
    }

    @Test
    @DisplayName("400: cuando los datos del DTO son inválidos")
    void deberiaFallarConDatosInvalidos() throws Exception {
        String jsonRequest = "{\"codigoPatente\":\"INVALIDO\",\"titulo\":\"\"}";

        mockMvc.perform(post("/patentes/busqueda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("409: al intentar registrar una patente duplicada")
    void deberiaFallarConPatenteDuplicada() throws Exception {
        when(patenteService.registrar(any(Patente.class)))
                .thenThrow(new PatenteDuplicadaException("La patente ya existe"));

        String jsonRequest = "{\"codigoPatente\":\"PAT-123456\",\"titulo\":\"Cuchilla láser\"}";

        mockMvc.perform(post("/patentes/busqueda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("200: obtiene el estado de una patente por ID")
    void deberiaObtenerEstadoExitosamente() throws Exception {
        Patente patente = Patente.builder()
                .patenteId(1L)
                .codigoPatente("PAT-123456")
                .titulo("Cuchilla láser")
                .estadoVigencia(EstadoVigenciaPatente.EN_TRAMITE)
                .build();

        when(patenteService.obtenerPorId(1L)).thenReturn(patente);

        mockMvc.perform(get("/patentes/estado/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patenteId").value(1))
                .andExpect(jsonPath("$.codigoPatente").value("PAT-123456"));
    }

    @Test
    @DisplayName("404: cuando la patente no existe por ID")
    void deberiaFallarCuandoPatenteNoExiste() throws Exception {
        when(patenteService.obtenerPorId(99L))
                .thenThrow(new PatenteNoEncontradaException("Patente no encontrada"));

        mockMvc.perform(get("/patentes/estado/99"))
                .andExpect(status().isNotFound());
    }
}
