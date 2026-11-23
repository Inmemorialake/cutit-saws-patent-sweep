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
    @DisplayName("Carga el usuario activo con su rol")
    void deberiaCargarUnUsuarioActivo() {
        // Arrange
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(
                Usuario.builder().username("admin").passwordHash("hash").rol(Rol.ADMINISTRADOR).activo(true).build()));

        // Act
        UserDetails detalles = servicio.loadUserByUsername("admin");

        // Assert
        assertThat(detalles.getUsername()).isEqualTo("admin");
        assertThat(detalles.getPassword()).isEqualTo("hash");
        assertThat(detalles.isEnabled()).isTrue();
        assertThat(detalles.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMINISTRADOR");
    }

    @Test
    @DisplayName("Un usuario inactivo queda deshabilitado")
    void deberiaDeshabilitarUnUsuarioInactivo() {
        // Arrange
        when(usuarioRepository.findByUsername("baja")).thenReturn(Optional.of(
                Usuario.builder().username("baja").passwordHash("hash").rol(Rol.SERVICIO).activo(false).build()));

        // Act
        UserDetails detalles = servicio.loadUserByUsername("baja");

        // Assert
        assertThat(detalles.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Un usuario inexistente lanza UsernameNotFoundException")
    void deberiaFallarConUnUsuarioInexistente() {
        // Arrange
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> servicio.loadUserByUsername("nadie")).isInstanceOf(UsernameNotFoundException.class);
    }
}
