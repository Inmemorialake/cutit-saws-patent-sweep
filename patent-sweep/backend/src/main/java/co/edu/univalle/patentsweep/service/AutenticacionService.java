package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.config.JwtProvider;
import co.edu.univalle.patentsweep.model.enums.Rol;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/** Autentica usuarios existentes y emite un JWT con su identidad y rol. */
@Service
public class AutenticacionService {

    private static final String PREFIJO_ROL = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public AutenticacionService(AuthenticationManager authenticationManager, JwtProvider jwtProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    public ResultadoAutenticacion autenticar(String username, String password) {
        Authentication autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        String usernameAutenticado = autenticacion.getName();
        Rol rol = obtenerRol(autenticacion);
        String token = jwtProvider.generarToken(usernameAutenticado, rol);

        return new ResultadoAutenticacion(
                token,
                jwtProvider.getExpiracionMinutos() * 60,
                usernameAutenticado,
                rol
        );
    }

    private Rol obtenerRol(Authentication autenticacion) {
        Optional<String> rol = autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridad -> autoridad.startsWith(PREFIJO_ROL))
                .map(autoridad -> autoridad.substring(PREFIJO_ROL.length()))
                .findFirst();
        return rol.map(Rol::valueOf)
                .orElseThrow(() -> new IllegalStateException("El usuario autenticado no tiene un rol asignado"));
    }

    public record ResultadoAutenticacion(String accessToken, long expiresIn, String username, Rol rol) {
    }
}
