package co.edu.univalle.patentsweep.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import co.edu.univalle.patentsweep.model.Usuario;
import co.edu.univalle.patentsweep.model.enums.Rol;
import co.edu.univalle.patentsweep.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService servicio;

    @Test
    @DisplayName("carga un usuario activo con su rol")
    void deberiaCargarUsuarioActivo() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(Usuario.builder()
                .username("admin")
                .passwordHash("hash")
                .rol(Rol.ADMINISTRADOR)
                .activo(true)
                .build()));

        UserDetails usuario = servicio.loadUserByUsername("admin");

        assertThat(usuario.getUsername()).isEqualTo("admin");
        assertThat(usuario.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMINISTRADOR");
        assertThat(usuario.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("falla cuando el usuario no existe")
    void deberiaFallarCuandoNoExiste() {
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
