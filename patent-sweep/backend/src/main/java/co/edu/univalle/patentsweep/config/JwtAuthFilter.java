package co.edu.univalle.patentsweep.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Autorizador (P26): valida el JWT de cada petición antes de llegar a los controladores. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String encabezado = peticion.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER)) {
            String token = encabezado.substring(PREFIJO_BEARER.length());
            if (jwtProvider.esValido(token)) {
                var autenticacion = new UsernamePasswordAuthenticationToken(
                        jwtProvider.obtenerUsuario(token), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + jwtProvider.obtenerRol(token))));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        }
        cadena.doFilter(peticion, respuesta);
    }
}
