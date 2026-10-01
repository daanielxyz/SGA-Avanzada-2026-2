package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Objeto de valor: Estancia definida por una fecha de entrada y salida.
 */
public record Estancia(LocalDate entrada, LocalDate salida) {

    public Estancia {
        Objects.requireNonNull(entrada, "La fecha de entrada no puede ser nula");
        Objects.requireNonNull(salida, "La fecha de salida no puede ser nula");
        if (!salida.isAfter(entrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de entrada");
        }
    }

    /**
     * Genera la lista inmutable de noches que componen la estancia.
     */
    public List<Noche> noches() {
        List<Noche> listaNoches = new ArrayList<>();
        LocalDate actual = entrada;
        while (actual.isBefore(salida)) {
            listaNoches.add(new Noche(actual));
            actual = actual.plusDays(1);
        }
        return Collections.unmodifiableList(listaNoches);
    }

    /**
     * Devuelve el total de noches entre entrada y salida.
     */
    public int cantidadNoches() {
        return (int) ChronoUnit.DAYS.between(entrada, salida);
    }

    /**
     * Determina si dos estancias se solapan en al menos una noche.
     */
    public boolean seSolapaCon(Estancia otra) {
        Objects.requireNonNull(otra, "La otra estancia no puede ser nula");
        return this.entrada.isBefore(otra.salida) && otra.entrada.isBefore(this.salida);
    }
}
