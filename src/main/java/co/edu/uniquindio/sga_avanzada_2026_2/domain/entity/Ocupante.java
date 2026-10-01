package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.OcupanteId;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.Optional;

/**
 * Entidad (Hija de Reserva): Representa un ocupante alojado en la reserva.
 */
public class Ocupante {

    private final OcupanteId id;
    private final String nombre;
    private final LocalDate fechaNacimiento;
    private final Documento documento;
    private final String nacionalidad;

    private Ocupante(OcupanteId id, String nombre, LocalDate fechaNacimiento, Documento documento, String nacionalidad) {
        this.id = id;
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
        this.documento = documento;
        this.nacionalidad = nacionalidad;
    }

    public static Ocupante crear(OcupanteId id, String nombre, LocalDate fechaNacimiento,
                                 Documento documento, String nacionalidad) {
        Objects.requireNonNull(id, "El id del ocupante no puede ser nulo");
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del ocupante no puede estar vacío");
        }
        Objects.requireNonNull(fechaNacimiento, "La fecha de nacimiento no puede ser nula");
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            throw new ReglaDominioException("La fecha de nacimiento no puede ser en el futuro");
        }

        return new Ocupante(id, nombre.trim(), fechaNacimiento, documento,
                nacionalidad != null ? nacionalidad.trim() : null);
    }

    public int edadA(LocalDate fecha) {
        Objects.requireNonNull(fecha, "La fecha para calcular edad no puede ser nula");
        if (fecha.isBefore(fechaNacimiento)) {
            return 0;
        }
        return Period.between(fechaNacimiento, fecha).getYears();
    }

    public boolean esFacturableA(LocalDate entrada, int umbral) {
        return edadA(entrada) >= umbral;
    }

    public OcupanteId getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public Optional<Documento> getDocumento() {
        return Optional.ofNullable(documento);
    }

    public Optional<String> getNacionalidad() {
        return Optional.ofNullable(nacionalidad);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ocupante ocupante = (Ocupante) o;
        return Objects.equals(id, ocupante.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
