package co.edu.univalle.patentsweep.config;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.patentsweep.model.enums.Rol;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRETO = "clave-de-prueba-de-al-menos-32-caracteres";

    private Instant ahora;
    private JwtProvider jwtProvider;

    @BeforeEach
    void preparar() {
        ahora = Instant.parse("2026-01-01T00:00:00Z");
        jwtProvider = new JwtProvider(SECRETO, 60, Clock.fixed(ahora, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("genera un token válido con usuario y rol")
    void deberiaGenerarTokenValido() {
        String token = jwtProvider.generarToken("servicio", Rol.SERVICIO);

        assertThat(jwtProvider.esValido(token)).isTrue();
        assertThat(jwtProvider.obtenerUsuario(token)).isEqualTo("servicio");
        assertThat(jwtProvider.obtenerRol(token)).isEqualTo("SERVICIO");
        assertThat(jwtProvider.getExpiracionMinutos()).isEqualTo(60);
    }

    @Test
    @DisplayName("rechaza un token malformado")
    void deberiaRechazarTokenMalformado() {
        assertThat(jwtProvider.esValido("token.no.valido")).isFalse();
    }

    @Test
    @DisplayName("rechaza un token vencido")
    void deberiaRechazarTokenVencido() {
        String token = jwtProvider.generarToken("servicio", Rol.SERVICIO);
        JwtProvider despues = new JwtProvider(SECRETO, 60,
                Clock.fixed(ahora.plusSeconds(3601), ZoneOffset.UTC));

        assertThat(despues.esValido(token)).isFalse();
    }
}
