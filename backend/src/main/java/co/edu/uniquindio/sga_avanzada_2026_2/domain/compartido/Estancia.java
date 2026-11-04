package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * VO: intervalo [entrada, salida) en fechas de Colombia, sin hora (EST-01, EST-06).
 * La noche de salida no se ocupa ni se cobra (NOC-02). Inmutable (EST-05).
 */
public record Estancia(LocalDate entrada, LocalDate salida) {

    // RN-03 · EST-02
    public Estancia {
        if (entrada == null || salida == null) {
            throw new ReglaDominioException("Entrada y salida son obligatorias");
        }
        if (!salida.isAfter(entrada)) {
            throw new ReglaDominioException("La salida debe ser posterior a la entrada");
        }
    }

    /**
     * Noches vendibles de la estancia, de la entrada a la víspera de la salida (EST-01 · NOC-02).
     *
     * @return lista inmutable y ordenada; del 10 al 12 devuelve las noches 10 y 11
     */
    public List<Noche> noches() {
        return entrada.datesUntil(salida).map(Noche::new).toList();
    }

    /**
     * Cantidad de noches = días calendario entre entrada y salida (EST-04).
     *
     * @return al menos 1; del 10 al 12 son 2
     */
    public int cantidadNoches() {
        return (int) ChronoUnit.DAYS.between(entrada, salida);
    }

    /**
     * Única definición válida de solapamiento del sistema: entradaA &lt; salidaB y entradaB &lt; salidaA
     * (EST-03 · RN-01). Estancias contiguas (una sale el día que la otra entra) no se solapan.
     *
     * @param otra estancia a comparar
     * @return {@code true} si comparten al menos una noche
     */
    public boolean seSolapaCon(Estancia otra) {
        Objects.requireNonNull(otra, "otra");
        return entrada.isBefore(otra.salida) && otra.entrada.isBefore(salida);
    }
}
