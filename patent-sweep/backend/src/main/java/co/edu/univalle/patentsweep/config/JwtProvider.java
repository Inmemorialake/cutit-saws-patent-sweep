package co.edu.univalle.patentsweep.config;

import co.edu.univalle.patentsweep.model.enums.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Emisión y validación del JWT de sesión (P26 Autenticador + autorizador, DA-11). */
@Component
public class JwtProvider {

    private static final String RECLAMO_ROL = "rol";

    private final SecretKey clave;
    private final Duration expiracion;
    private final Clock reloj;

    public JwtProvider(@Value("${patentsweep.seguridad.jwt-secreto}") String secreto,
                       @Value("${patentsweep.seguridad.jwt-expiracion-minutos}") long expiracionMinutos,
                       Clock reloj) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracion = Duration.ofMinutes(expiracionMinutos);
        this.reloj = reloj;
    }

    public String generarToken(String username, Rol rol) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .subject(username)
                .claim(RECLAMO_ROL, rol.name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(clave)
                .compact();
    }

    public long getExpiracionMinutos() {
        return expiracion.toMinutes();
    }

    /** Devuelve los reclamos si la firma y la vigencia son válidas; de lo contrario lanza JwtException. */
    public Claims leer(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .clock(() -> Date.from(reloj.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esValido(String token) {
        try {
            leer(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String obtenerUsuario(String token) {
        return leer(token).getSubject();
    }

    public String obtenerRol(String token) {
        return leer(token).get(RECLAMO_ROL, String.class);
    }
}
