package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.BloqueoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Noche;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad (Hija de Apartamento): Representa un bloqueo temporal de disponibilidad.
 */
public class Bloqueo {

    private final BloqueoId id;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final String motivo;
    private boolean vigente;

    private Bloqueo(BloqueoId id, LocalDate fechaInicio, LocalDate fechaFin, String motivo, boolean vigente) {
        this.id = id;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.motivo = motivo;
        this.vigente = vigente;
    }

    public static Bloqueo crear(BloqueoId id, LocalDate fechaInicio, LocalDate fechaFin, String motivo) {
        Objects.requireNonNull(id, "El id del bloqueo no puede ser nulo");
        Objects.requireNonNull(fechaInicio, "La fecha de inicio del bloqueo no puede ser nula");
        Objects.requireNonNull(fechaFin, "La fecha de fin del bloqueo no puede ser nula");
        if (fechaFin.isBefore(fechaInicio)) {
            throw new ReglaDominioException("La fecha fin del bloqueo no puede ser anterior a la fecha de inicio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("El motivo del bloqueo no puede estar vacío");
        }
        return new Bloqueo(id, fechaInicio, fechaFin, motivo.trim(), true);
    }

    public void levantar() {
        if (!this.vigente) {
            throw new ReglaDominioException("El bloqueo ya no se encuentra vigente");
        }
        this.vigente = false;
    }

    public boolean cubre(Noche n) {
        Objects.requireNonNull(n, "La noche a evaluar no puede ser nula");
        return this.vigente && !n.fecha().isBefore(fechaInicio) && !n.fecha().isAfter(fechaFin);
    }

    public boolean impide(Estancia estancia) {
        Objects.requireNonNull(estancia, "La estancia no puede ser nula");
        return this.vigente && estancia.noches().stream().anyMatch(this::cubre);
    }

    public BloqueoId getId() {
        return id;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public String getMotivo() {
        return motivo;
    }

    public boolean isVigente() {
        return vigente;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bloqueo bloqueo = (Bloqueo) o;
        return Objects.equals(id, bloqueo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
