package co.edu.univalle.patentsweep.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.model.Usuario;
import co.edu.univalle.patentsweep.model.enums.Rol;
import co.edu.univalle.patentsweep.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioSeedRunnerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder codificador;

    @Test
    @DisplayName("Crea los dos usuarios iniciales con la clave codificada")
    void deberiaCrearLosUsuariosIniciales() {
        // Arrange
        when(usuarioRepository.existsByUsername(any())).thenReturn(false);
        when(codificador.encode("clave-admin")).thenReturn("hash-admin");
        when(codificador.encode("clave-servicio")).thenReturn("hash-servicio");
        UsuarioSeedRunner ejecutor = new UsuarioSeedRunner(usuarioRepository, codificador,
                "admin", "clave-admin", "servicio", "clave-servicio");

        // Act
        ejecutor.run(null);

        // Assert
        ArgumentCaptor<Usuario> guardados = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(2)).save(guardados.capture());
        assertThat(guardados.getAllValues()).extracting(Usuario::getUsername).containsExactly("admin", "servicio");
        assertThat(guardados.getAllValues()).extracting(Usuario::getRol)
                .containsExactly(Rol.ADMINISTRADOR, Rol.SERVICIO);
        assertThat(guardados.getAllValues()).extracting(Usuario::getPasswordHash)
                .containsExactly("hash-admin", "hash-servicio");
    }

    @Test
    @DisplayName("No crea usuarios cuando las variables están vacías")
    void noDeberiaCrearUsuariosSinVariables() {
        // Arrange
        UsuarioSeedRunner ejecutor = new UsuarioSeedRunner(usuarioRepository, codificador, "", "", " ", "");

        // Act
        ejecutor.run(null);

        // Assert
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("No crea un usuario sin clave ni repite uno que ya existe")
    void noDeberiaCrearSinClaveNiRepetir() {
        // Arrange
        when(usuarioRepository.existsByUsername("admin")).thenReturn(true);
        UsuarioSeedRunner ejecutor = new UsuarioSeedRunner(usuarioRepository, codificador,
                "admin", "clave-admin", "servicio", "");

        // Act
        ejecutor.run(null);

        // Assert
        verify(usuarioRepository, never()).save(any());
    }
}
