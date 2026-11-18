package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.exception.PatenteDuplicadaException;
import co.edu.univalle.patentsweep.exception.PatenteNoEncontradaException;
import co.edu.univalle.patentsweep.model.Patente;
import co.edu.univalle.patentsweep.repository.PatenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso base del Arquetipo (Sprint 0): registro y consulta simple de patentes.
 * La regla de negocio vive aquí; el controller no accede a la base de datos.
 */
@Service
public class PatenteService {

    private final PatenteRepository patenteRepository;

    public PatenteService(PatenteRepository patenteRepository) {
        this.patenteRepository = patenteRepository;
    }

    /** Registra la patente; lanza 409 si el código ya existe. */
    @Transactional
    public Patente registrar(Patente patente) {
        if (patenteRepository.findByCodigoPatente(patente.getCodigoPatente()).isPresent()) {
            throw new PatenteDuplicadaException("Ya existe una patente registrada con el código: " + patente.getCodigoPatente());
        }
        return patenteRepository.save(patente);
    }

    /** Busca por ID; lanza 404 si no existe. */
    @Transactional(readOnly = true)
    public Patente obtenerPorId(Long id) {
        return patenteRepository.findById(id)
                .orElseThrow(() -> new PatenteNoEncontradaException("Patente no encontrada con ID: " + id));
    }
}
