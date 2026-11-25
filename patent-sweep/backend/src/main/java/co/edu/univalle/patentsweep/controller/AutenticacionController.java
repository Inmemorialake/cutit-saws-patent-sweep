package co.edu.univalle.patentsweep.controller;

import co.edu.univalle.patentsweep.controller.dto.LoginRequestDTO;
import co.edu.univalle.patentsweep.controller.dto.LoginResponseDTO;
import co.edu.univalle.patentsweep.service.AutenticacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inicio de sesión para usuarios existentes (HU-00, P26). */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y emisión de JWT")
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener un JWT")
    @ApiResponse(responseCode = "200", description = "Credenciales válidas; devuelve el token y su vencimiento")
    @ApiResponse(responseCode = "400", description = "Falta el nombre de usuario o la contraseña")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario inactivo")
    public ResponseEntity<LoginResponseDTO> iniciarSesion(@Valid @RequestBody LoginRequestDTO solicitud) {
        var sesion = autenticacionService.autenticar(solicitud.username(), solicitud.password());
        return ResponseEntity.ok(new LoginResponseDTO(
                sesion.accessToken(),
                "Bearer",
                sesion.expiresIn(),
                sesion.username(),
                sesion.rol().name()
        ));
    }
}
