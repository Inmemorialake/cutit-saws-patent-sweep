package co.edu.univalle.patentsweep.controller;

import co.edu.univalle.patentsweep.config.JwtProvider;
import co.edu.univalle.patentsweep.controller.dto.LoginRequestDTO;
import co.edu.univalle.patentsweep.controller.dto.LoginResponseDTO;
import co.edu.univalle.patentsweep.model.enums.Rol;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autenticador (P26): login básico que emite el JWT (DAS DA-11, S-06). Sin OAuth2, SSO ni MFA. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y emisión del token JWT")
public class AuthController {

    private static final String PREFIJO_ROL = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtProvider jwtProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Valida las credenciales y devuelve un JWT para usar como Bearer en los demás endpoints.")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada")
    @ApiResponse(responseCode = "400", description = "Falta el usuario o la clave")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    public ResponseEntity<LoginResponseDTO> iniciarSesion(@Valid @RequestBody LoginRequestDTO solicitud) {
        Authentication autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(solicitud.usuario(), solicitud.clave()));
        String autoridad = autenticacion.getAuthorities().iterator().next().getAuthority();
        Rol rol = Rol.valueOf(autoridad.substring(PREFIJO_ROL.length()));
        String token = jwtProvider.generarToken(autenticacion.getName(), rol);
        return ResponseEntity.ok(new LoginResponseDTO(token, "Bearer", jwtProvider.getExpiracionMinutos(), rol.name()));
    }
}
