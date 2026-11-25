package co.edu.univalle.patentsweep.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @DisplayName("crea los usuarios iniciales con los roles configurados")
    void deberiaCrearUsuariosIniciales() {
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);
        when(usuarioRepository.existsByUsername("servicio")).thenReturn(false);
        when(codificador.encode("clave")).thenReturn("hash");
        UsuarioSeedRunner semilla = new UsuarioSeedRunner(usuarioRepository, codificador,
                "admin", "clave", "servicio", "clave");

        semilla.run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Usuario::getRol)
                .containsExactly(Rol.ADMINISTRADOR, Rol.SERVICIO);
        assertThat(captor.getAllValues()).allSatisfy(usuario -> {
            assertThat(usuario.getPasswordHash()).isEqualTo("hash");
            assertThat(usuario.isActivo()).isTrue();
        });
    }

    @Test
    @DisplayName("no crea usuarios cuando las variables están vacías")
    void noDeberiaCrearUsuariosSinConfiguracion() {
        UsuarioSeedRunner semilla = new UsuarioSeedRunner(usuarioRepository, codificador, "", "", "", "");

        semilla.run(null);

        verifyNoInteractions(usuarioRepository, codificador);
    }
}
