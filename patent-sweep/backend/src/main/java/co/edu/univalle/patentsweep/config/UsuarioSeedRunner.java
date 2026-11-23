package co.edu.univalle.patentsweep.config;

import co.edu.univalle.patentsweep.model.Usuario;
import co.edu.univalle.patentsweep.model.enums.Rol;
import co.edu.univalle.patentsweep.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * [Archetype Convention Addition] Crea los usuarios iniciales ADMINISTRADOR y SERVICIO a partir
 * de variables de entorno, porque el DAS no define cómo se crea el primer usuario (secrets fuera
 * del código: OWASP A07). Si la variable no está definida, el usuario no se crea.
 */
@Component
public class UsuarioSeedRunner implements ApplicationRunner {

    private static final Logger REGISTRO = LoggerFactory.getLogger(UsuarioSeedRunner.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder codificador;
    private final String adminUsuario;
    private final String adminClave;
    private final String servicioUsuario;
    private final String servicioClave;

    public UsuarioSeedRunner(UsuarioRepository usuarioRepository, PasswordEncoder codificador,
                             @Value("${patentsweep.seguridad.admin-usuario}") String adminUsuario,
                             @Value("${patentsweep.seguridad.admin-clave}") String adminClave,
                             @Value("${patentsweep.seguridad.servicio-usuario}") String servicioUsuario,
                             @Value("${patentsweep.seguridad.servicio-clave}") String servicioClave) {
        this.usuarioRepository = usuarioRepository;
        this.codificador = codificador;
        this.adminUsuario = adminUsuario;
        this.adminClave = adminClave;
        this.servicioUsuario = servicioUsuario;
        this.servicioClave = servicioClave;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        crearSiNoExiste(adminUsuario, adminClave, Rol.ADMINISTRADOR);
        crearSiNoExiste(servicioUsuario, servicioClave, Rol.SERVICIO);
    }

    private void crearSiNoExiste(String username, String clave, Rol rol) {
        if (username.isBlank() || clave.isBlank() || usuarioRepository.existsByUsername(username)) {
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .username(username)
                .passwordHash(codificador.encode(clave))
                .rol(rol)
                .activo(true)
                .build());
        REGISTRO.info("Usuario inicial creado con rol {}", rol);
    }
}
