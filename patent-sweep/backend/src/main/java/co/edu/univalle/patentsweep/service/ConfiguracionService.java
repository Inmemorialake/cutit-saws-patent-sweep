package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.exception.ConfiguracionNoEncontradaException;
import co.edu.univalle.patentsweep.model.Configuracion;
import co.edu.univalle.patentsweep.repository.ConfiguracionRepository;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Criterios y frecuencia vigentes: solo lectura en HU-01/HU-02 (HU-10 añadirá el PUT). */
@Service
public class ConfiguracionService {

    private static final Duration FRECUENCIA_POR_DEFECTO = Duration.ofDays(1);
    private static final String UNIDAD_HORAS = "HORAS";
    private static final String UNIDAD_SEMANAS = "SEMANAS";

    private final ConfiguracionRepository configuracionRepository;

    public ConfiguracionService(ConfiguracionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    public Configuracion criteriosVigentes() {
        return configuracionRepository.findByActivaTrue().orElseThrow(ConfiguracionNoEncontradaException::new);
    }

    /**
     * Frecuencia del planificador (P08, HU-01-1A). Sin configuración activa se aplica 1 día.
     * Se lee en cada ciclo, así que un cambio aplica en la siguiente ejecución sin reiniciar.
     */
    public Duration frecuenciaVigente() {
        return configuracionRepository.findByActivaTrue()
                .map(this::aDuracion)
                .orElse(FRECUENCIA_POR_DEFECTO);
    }

    private Duration aDuracion(Configuracion configuracion) {
        int valor = configuracion.getFrecuenciaValor() > 0 ? configuracion.getFrecuenciaValor() : 1;
        String unidad = configuracion.getFrecuenciaUnidad() == null
                ? "DIAS" : configuracion.getFrecuenciaUnidad().toUpperCase(Locale.ROOT);
        return switch (unidad) {
            case UNIDAD_HORAS -> Duration.ofHours(valor);
            case UNIDAD_SEMANAS -> Duration.ofDays(valor * 7L);
            default -> Duration.ofDays(valor);
        };
    }
}
