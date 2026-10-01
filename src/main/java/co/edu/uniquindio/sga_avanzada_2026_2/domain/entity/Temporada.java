package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TemporadaId;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad Raíz: Agregado CalendarioTemporadas (Temporada turística).
 */
public class Temporada {

    private final TemporadaId id;
    private final String nombre;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final boolean esBase;
    private final int estanciaMinimaNoches;

    private Temporada(TemporadaId id, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                      boolean esBase, int estanciaMinimaNoches) {
        this.id = id;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.esBase = esBase;
        this.estanciaMinimaNoches = estanciaMinimaNoches;
    }

    public static Temporada crear(TemporadaId id, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                  boolean esBase, int estanciaMinimaNoches) {
        Objects.requireNonNull(id, "El id de la temporada no puede ser nulo");
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre de la temporada no puede estar vacío");
        }
        Objects.requireNonNull(fechaInicio, "La fecha de inicio de temporada no puede ser nula");
        Objects.requireNonNull(fechaFin, "La fecha de fin de temporada no puede ser nula");
        if (fechaFin.isBefore(fechaInicio)) {
            throw new ReglaDominioException("La fecha fin no puede ser anterior a la fecha de inicio");
        }
        if (estanciaMinimaNoches < 1) {
            throw new ReglaDominioException("La estancia mínima debe ser de al menos 1 noche");
        }

        return new Temporada(id, nombre.trim(), fechaInicio, fechaFin, esBase, estanciaMinimaNoches);
    }

    public boolean cubre(Noche n) {
        Objects.requireNonNull(n, "La noche no puede ser nula");
        return !n.fecha().isBefore(fechaInicio) && !n.fecha().isAfter(fechaFin);
    }

    public boolean seSolapaCon(Temporada otra) {
        Objects.requireNonNull(otra, "La otra temporada no puede ser nula");
        return !this.fechaInicio.isAfter(otra.fechaFin) && !otra.fechaInicio.isAfter(this.fechaFin);
    }

    public TemporadaId getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public boolean isEsBase() {
        return esBase;
    }

    public int getEstanciaMinimaNoches() {
        return estanciaMinimaNoches;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Temporada temporada = (Temporada) o;
        return Objects.equals(id, temporada.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
