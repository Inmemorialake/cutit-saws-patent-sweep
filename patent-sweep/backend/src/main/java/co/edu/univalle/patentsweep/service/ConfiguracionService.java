package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.exception.ConfiguracionNoEncontradaException;
import co.edu.univalle.patentsweep.model.Configuracion;
import co.edu.univalle.patentsweep.repository.ConfiguracionRepository;
import org.springframework.stereotype.Service;

/** Criterios de búsqueda vigentes: solo lectura en HU-02 (HU-10 añadirá el PUT). */
@Service
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;

    public ConfiguracionService(ConfiguracionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    public Configuracion criteriosVigentes() {
        return configuracionRepository.findByActivaTrue().orElseThrow(ConfiguracionNoEncontradaException::new);
    }
}
