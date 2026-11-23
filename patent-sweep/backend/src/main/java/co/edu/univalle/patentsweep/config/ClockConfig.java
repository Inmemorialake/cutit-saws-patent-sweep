package co.edu.univalle.patentsweep.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj inyectable: permite probar la lógica que depende de la fecha actual. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
