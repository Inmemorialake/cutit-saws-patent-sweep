package co.edu.univalle.patentsweep.controller;

import co.edu.univalle.patentsweep.controller.dto.BusquedaRequestDTO;
import co.edu.univalle.patentsweep.controller.dto.BusquedaResponseDTO;
import co.edu.univalle.patentsweep.controller.mapper.ConflictoMapper;
import co.edu.univalle.patentsweep.model.Conflicto;
import co.edu.univalle.patentsweep.service.BusquedaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Búsqueda manual de patentes en conflicto con una fórmula (HU-02): {@code POST /api/v1/patentes/buscar-conflictos}. */
@RestController
@RequestMapping("/api/v1/patentes")
@Tag(name = "Búsqueda de patentes", description = "Detección manual de conflictos entre fórmulas y patentes")
public class BusquedaController {

    private final BusquedaService busquedaService;
    private final ConflictoMapper conflictoMapper;

    public BusquedaController(BusquedaService busquedaService, ConflictoMapper conflictoMapper) {
        this.busquedaService = busquedaService;
        this.conflictoMapper = conflictoMapper;
    }

    @PostMapping("/buscar-conflictos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Buscar patentes en conflicto con una fórmula",
            description = "Consulta Materiales/Fórmulas, busca en la fuente de patentes, aplica el motor de "
                    + "coincidencias y deja rastro en el historial."
    )
    @ApiResponse(responseCode = "200", description = "Búsqueda realizada con éxito (con o sin conflictos)")
    @ApiResponse(responseCode = "400", description = "Código de fórmula inválido")
    @ApiResponse(responseCode = "401", description = "Sin token JWT")
    @ApiResponse(responseCode = "403", description = "Sin rol ADMINISTRADOR")
    @ApiResponse(responseCode = "404", description = "La fórmula no existe en Materiales/Fórmulas")
    @ApiResponse(responseCode = "409", description = "La fórmula está DESCONTINUADA")
    @ApiResponse(responseCode = "503", description = "Fallo técnico en una API externa tras los reintentos")
    public ResponseEntity<BusquedaResponseDTO> buscar(@Valid @RequestBody BusquedaRequestDTO solicitud) {
        String usuarioSesion = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Conflicto> conflictos = busquedaService.buscar(solicitud.formulaId(), usuarioSesion);
        return ResponseEntity.ok(conflictoMapper.aRespuestaDeBusqueda(solicitud.formulaId(), conflictos));
    }
}
