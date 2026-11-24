package co.edu.univalle.patentsweep.service;

import co.edu.univalle.patentsweep.integration.dto.ComponenteExternoDTO;
import co.edu.univalle.patentsweep.integration.dto.FormulaExternaDTO;
import co.edu.univalle.patentsweep.integration.dto.PatenteExternaDTO;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Motor de coincidencias. Regla del PB (HU-01, criterio 6): una patente es un conflicto potencial
 * cuando su título, resumen o reivindicaciones mencionan al menos el 50 % de los nombres de los
 * materiales de la fórmula (mínimo 2). Solo se compara por nombre; no se evalúan porcentajes.
 */
@Component
public class MotorCoincidencias {

    private final int porcentajeMinimo;
    private final int minimoMateriales;

    public MotorCoincidencias(@Value("${patentsweep.coincidencia.porcentaje-minimo}") int porcentajeMinimo,
                              @Value("${patentsweep.coincidencia.minimo-materiales}") int minimoMateriales) {
        this.porcentajeMinimo = porcentajeMinimo;
        this.minimoMateriales = minimoMateriales;
    }

    /** Términos para la búsqueda en la fuente: nombres de materiales y aplicación técnica (PB HU-01, 4.1). */
    public List<String> terminosDeBusqueda(FormulaExternaDTO formula) {
        Set<String> terminos = new LinkedHashSet<>(nombresMateriales(formula));
        if (formula.aplicacionTecnica() != null && !formula.aplicacionTecnica().isBlank()) {
            terminos.add(formula.aplicacionTecnica().trim());
        }
        return new ArrayList<>(terminos);
    }

    /** Devuelve las patentes candidatas que cumplen la regla de coincidencia. */
    public List<PatenteExternaDTO> filtrarConflictos(FormulaExternaDTO formula,
                                                     List<PatenteExternaDTO> candidatas) {
        List<String> materiales = nombresMateriales(formula).stream().map(MotorCoincidencias::normalizar).toList();
        int requeridos = Math.max(minimoMateriales, (int) Math.ceil(materiales.size() * porcentajeMinimo / 100.0));
        if (materiales.size() < requeridos) {
            return List.of();
        }
        return candidatas.stream()
                .filter(patente -> contarMencionados(materiales, textoDe(patente)) >= requeridos)
                .toList();
    }

    private static List<String> nombresMateriales(FormulaExternaDTO formula) {
        if (formula.componentes() == null) {
            return List.of();
        }
        Set<String> nombres = new LinkedHashSet<>();
        for (ComponenteExternoDTO componente : formula.componentes()) {
            if (componente.nombreMaterial() != null && !componente.nombreMaterial().isBlank()) {
                nombres.add(componente.nombreMaterial().trim());
            }
        }
        return new ArrayList<>(nombres);
    }

    private static long contarMencionados(List<String> materiales, String texto) {
        return materiales.stream().filter(texto::contains).count();
    }

    private static String textoDe(PatenteExternaDTO patente) {
        String reivindicaciones = patente.reivindicaciones() == null
                ? "" : String.join(" ", patente.reivindicaciones());
        return normalizar(Objects.toString(patente.tituloPatente(), "") + " "
                + Objects.toString(patente.resumen(), "") + " " + reivindicaciones);
    }

    /** Minúsculas y sin tildes, para que "Níquel" y "niquel" se consideren el mismo material. */
    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
