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

/** Transacción del arquetipo Sprint 0: registrar una patente y consultarla por su ID. */
@RestController
@RequestMapping("/api/v1/patentes")
@Tag(name = "Patentes", description = "Registro y consulta de patentes (arquetipo Sprint 0)")
public class PatenteController {

    private final PatenteService patenteService;

    public PatenteController(PatenteService patenteService) {
        this.patenteService = patenteService;
    }

    /** POST /api/v1/patentes/busqueda: registra una patente (201, 400 o 409). */
    @PostMapping("/busqueda")
    @Operation(summary = "Registrar una patente",
            description = "Valida los datos, verifica que el código no exista y guarda la patente en estado EN_TRAMITE.")
    @ApiResponse(responseCode = "201", description = "Patente registrada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "Ya existe una patente con ese código")
    public ResponseEntity<PatenteResponseDTO> registrarPatente(@Valid @RequestBody PatenteRequestDTO request) {
        // @Valid ya validó el DTO. Se pasa a entidad; el estado EN_TRAMITE lo asigna la entidad al guardar.
        Patente patente = Patente.builder()
                .codigoPatente(request.codigoPatente())
                .titulo(request.titulo())
                .tipoProducto(request.tipoProducto())
                .resumen(request.resumen())
                .build();

        Patente guardada = patenteService.registrar(patente);
        return ResponseEntity.status(HttpStatus.CREATED).body(aRespuesta(guardada));
    }

    /** GET /api/v1/patentes/estado/{id}: consulta una patente por su ID (200 o 404). */
    @GetMapping("/estado/{id}")
    @Operation(summary = "Consultar una patente por su ID",
            description = "Devuelve los datos de la patente y su estado de vigencia.")
    @ApiResponse(responseCode = "200", description = "Patente encontrada")
    @ApiResponse(responseCode = "404", description = "Patente no encontrada")
    public ResponseEntity<PatenteResponseDTO> obtenerEstado(@PathVariable Long id) {
        return ResponseEntity.ok(aRespuesta(patenteService.obtenerPorId(id)));
    }

    /** Entidad a DTO: al cliente no se le expone la entidad. */
    private static PatenteResponseDTO aRespuesta(Patente patente) {
        return new PatenteResponseDTO(
                patente.getPatenteId(),
                patente.getCodigoPatente(),
                patente.getTitulo(),
                patente.getTipoProducto(),
                patente.getResumen(),
                patente.getEstadoVigencia().name());
    }
}
