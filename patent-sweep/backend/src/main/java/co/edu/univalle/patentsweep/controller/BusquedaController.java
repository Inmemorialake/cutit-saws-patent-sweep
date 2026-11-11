package co.edu.univalle.patentsweep.controller;

import co.edu.univalle.patentsweep.controller.dto.BusquedaRequestDTO;
import co.edu.univalle.patentsweep.controller.dto.BusquedaResponseDTO;
import co.edu.univalle.patentsweep.controller.mapper.ConflictoMapper;
import co.edu.univalle.patentsweep.service.BusquedaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la búsqueda manual de patentes en conflicto con una fórmula.
 * Expone la transacción principal del arquetipo.
 */
@RestController
@RequestMapping("/api/v1/patentes")
@Tag(name = "Búsqueda de patentes", description = "Endpoint transaccional principal de Patent Sweep")
public class BusquedaController {

    private final BusquedaService busquedaService;
    private final ConflictoMapper conflictoMapper;

    // Inyección de dependencias por constructor.
    public BusquedaController(BusquedaService busquedaService, ConflictoMapper conflictoMapper) {
        this.busquedaService = busquedaService;
        this.conflictoMapper = conflictoMapper;
    }

    @PostMapping("/busqueda")
    @Operation(
            summary = "Buscar patentes en conflicto con una fórmula",
            description = "Orquesta la consulta de la fórmula, la búsqueda en el mock de patentes, "
                    + "el motor de coincidencias y el registro de la auditoría en base de datos."
    )
    @ApiResponse(responseCode = "200", description = "Búsqueda realizada con éxito (conflictos detectados o vacíos)")
    @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos (Error de validación)")
    @ApiResponse(responseCode = "404", description = "La fórmula no existe en el sistema de Materiales")
    @ApiResponse(responseCode = "409", description = "La fórmula se encuentra DESCONTINUADA")
    @ApiResponse(responseCode = "503", description = "Falla técnica en la fuente externa o mock de patentes")
    public ResponseEntity<BusquedaResponseDTO> buscar(@Valid @RequestBody BusquedaRequestDTO solicitud) {

        // Se ejecuta el servicio pasando un usuario por defecto para el arquetipo (ej. "ingeniero.auditor").
        String usuarioSesion = "ingeniero.auditor";

        var conflictos = busquedaService.buscar(solicitud.formulaId(), usuarioSesion);

        return ResponseEntity.ok(conflictoMapper.aRespuestaDeBusqueda(solicitud.formulaId(), conflictos));
    }
}
