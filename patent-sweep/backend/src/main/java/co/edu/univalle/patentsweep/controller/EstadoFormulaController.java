package co.edu.univalle.patentsweep.controller;

import co.edu.univalle.patentsweep.config.OpenApiConfig;
import co.edu.univalle.patentsweep.controller.dto.EstadoFormulaResponseDTO;
import co.edu.univalle.patentsweep.controller.mapper.ConflictoMapper;
import co.edu.univalle.patentsweep.service.EstadoFormulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta del estado de una fórmula para Fabricación (PB HU-04). */
@RestController
@RequestMapping("/api/v1/patentes")
@Tag(name = "Estado de Fórmulas", description = "Consulta del estado operativo de fórmulas para Fabricación")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
@Validated
public class EstadoFormulaController {

    static final String MENSAJE_EXITO = "Estado de la fórmula consultado correctamente";

    private final EstadoFormulaService estadoFormulaService;
    private final ConflictoMapper mapper;

    public EstadoFormulaController(EstadoFormulaService estadoFormulaService, ConflictoMapper mapper) {
        this.estadoFormulaService = estadoFormulaService;
        this.mapper = mapper;
    }

    @GetMapping("/estado/{formulaId}")
    @Operation(summary = "Consultar el estado de una fórmula frente al barrido de patentes",
            description = "Permite a Fabricación verificar el estado de la fórmula para autorizar su "
                    + "producción. El estado se calcula en cada consulta (HU-04) y la fórmula se verifica "
                    + "en Materiales. El usuario responsable se toma del token de la sesión.")
    @ApiResponse(responseCode = "200", description = "Estado consultado exitosamente")
    @ApiResponse(responseCode = "400", description = "El formulaId no tiene el formato form-NNN")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "404", description = "La fórmula no existe")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<EstadoFormulaResponseDTO> consultarEstado(
            @PathVariable
            @Pattern(regexp = "^form-\\d{3}$", message = "El código de la fórmula es obligatorio y debe tener el formato form-NNN")
            String formulaId) {

        var resultado = estadoFormulaService.consultarEstado(formulaId);
        return ResponseEntity.ok(new EstadoFormulaResponseDTO(
                formulaId,
                resultado.estado(),
                resultado.fechaUltimaRevision(),
                resultado.alertasActivas().stream().map(mapper::aAlerta).toList(),
                MENSAJE_EXITO
        ));
    }
}
