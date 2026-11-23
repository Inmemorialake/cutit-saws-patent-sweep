package co.edu.univalle.patentsweep.config;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.patentsweep.model.enums.Rol;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    // Clave de prueba de 48 caracteres, generada solo para estas pruebas.
    private static final String SECRETO = "clave-solo-para-pruebas-unitarias-0123456789-abc";
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC);

    private final JwtProvider proveedor = new JwtProvider(SECRETO, 60, RELOJ);

    @Test
    @DisplayName("El token emitido es válido y conserva el usuario y el rol")
    void deberiaEmitirUnTokenValidoConUsuarioYRol() {
        // Act
        String token = proveedor.generarToken("admin", Rol.ADMINISTRADOR);

        // Assert
        assertThat(proveedor.esValido(token)).isTrue();
        assertThat(proveedor.obtenerUsuario(token)).isEqualTo("admin");
        assertThat(proveedor.obtenerRol(token)).isEqualTo("ADMINISTRADOR");
        assertThat(proveedor.getExpiracionMinutos()).isEqualTo(60);
    }

    @Test
    @DisplayName("Un token vencido deja de ser válido")
    void noDeberiaAceptarUnTokenVencido() {
        // Arrange
        String token = proveedor.generarToken("admin", Rol.ADMINISTRADOR);
        Clock dosHorasDespues = Clock.offset(RELOJ, Duration.ofHours(2));
        JwtProvider proveedorFuturo = new JwtProvider(SECRETO, 60, dosHorasDespues);

        // Act / Assert
        assertThat(proveedorFuturo.esValido(token)).isFalse();
    }

    @Test
    @DisplayName("Un token firmado con otra clave se rechaza")
    void noDeberiaAceptarUnTokenConOtraFirma() {
        // Arrange
        String token = new JwtProvider("otra-clave-distinta-para-pruebas-0123456789-xyz", 60, RELOJ)
                .generarToken("intruso", Rol.ADMINISTRADOR);

        // Act / Assert
        assertThat(proveedor.esValido(token)).isFalse();
    }

    @Test
    @DisplayName("Un texto que no es un token o está vacío se rechaza")
    void noDeberiaAceptarTextoQueNoEsUnToken() {
        assertThat(proveedor.esValido("esto-no-es-un-jwt")).isFalse();
        assertThat(proveedor.esValido("")).isFalse();
    }
}
