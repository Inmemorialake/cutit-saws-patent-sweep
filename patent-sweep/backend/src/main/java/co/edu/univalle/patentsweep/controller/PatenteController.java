package co.edu.univalle.patentsweep.controller;

import co.edu.univalle.patentsweep.controller.dto.PatenteRequestDTO;
import co.edu.univalle.patentsweep.controller.dto.PatenteResponseDTO;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.service.PatenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patentes")
@Tag(name = "Patentes", description = "Endpoint transaccional principal de Patent Sweep (Sprint 0)")
public class PatenteController {

    private final PatenteService patenteService;

    public PatenteController(PatenteService patenteService) {
        this.patenteService = patenteService;
    }

    @PostMapping("/busqueda")
    @Operation(summary = "Registrar o buscar patente en el arquetipo")
    @ApiResponse(responseCode = "201", description = "Patente procesada con éxito")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "Patente ya existe")
    public ResponseEntity<PatenteResponseDTO> registrarPatente(@Valid @RequestBody PatenteRequestDTO request) {
        Patente patente = Patente.builder()
                .codigoPatente(request.codigoPatente())
                .titulo(request.titulo())
                .tipoProducto(request.tipoProducto())
                .resumen(request.resumen())
                .build();

        Patente guardada = patenteService.registrar(patente);

        PatenteResponseDTO response = new PatenteResponseDTO(
                guardada.getPatenteId(),
                guardada.getCodigoPatente(),
                guardada.getTitulo(),
                guardada.getTipoProducto(),
                guardada.getResumen(),
                guardada.getEstadoVigencia().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/estado/{id}")
    @Operation(summary = "Consultar estado de patente por ID")
    @ApiResponse(responseCode = "200", description = "Patente encontrada")
    @ApiResponse(responseCode = "404", description = "Patente no encontrada")
    public ResponseEntity<PatenteResponseDTO> obtenerEstado(@PathVariable Long id) {
        Patente patente = patenteService.obtenerPorId(id);

        PatenteResponseDTO response = new PatenteResponseDTO(
                patente.getPatenteId(),
                patente.getCodigoPatente(),
                patente.getTitulo(),
                patente.getTipoProducto(),
                patente.getResumen(),
                patente.getEstadoVigencia().name()
        );

        return ResponseEntity.ok(response);
    }
}
