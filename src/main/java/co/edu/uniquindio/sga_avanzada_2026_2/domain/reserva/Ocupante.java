package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Entidad interna de Reserva. Se guarda la fecha de nacimiento, nunca la edad (OCU-01). Todos cuentan para la
 * capacidad (CAP-03); solo los facturables pagan (OCU-09). Que la fecha de nacimiento no sea futura (OCU-12) se
 * valida al agregarlo a la reserva, porque depende de la fecha actual (DEC-06).
 */
public class Ocupante {

    private final OcupanteId id;
    private final String nombre;
    private final LocalDate fechaNacimiento;
    private final Documento documento;   // opcional, salvo para el titular (TIT-01)
    private final String nacionalidad;   // opcional (OCU-07)

    // OCU-07
    public Ocupante(OcupanteId id, String nombre, LocalDate fechaNacimiento, Documento documento,
                    String nacionalidad) {
        if (id == null || fechaNacimiento == null) {
            throw new ReglaDominioException("El ocupante requiere id y fecha de nacimiento");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del ocupante es obligatorio");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.fechaNacimiento = fechaNacimiento;
        this.documento = documento;
        this.nacionalidad = nacionalidad;
    }

    /**
     * Años cumplidos a la fecha dada; se calcula, nunca se guarda (OCU-13 · OCU-01).
     */
    public int edadA(LocalDate fecha) {
        Objects.requireNonNull(fecha, "fecha");
        return Period.between(fechaNacimiento, fecha).getYears();
    }

    /**
     * Es facturable si a la fecha de entrada alcanza o supera el umbral configurado. Cumplir años durante la
     * estancia no lo cambia (RN-06 · OCU-03 · OCU-08 · OCU-11).
     *
     * @param fechaEntrada fecha de entrada de la estancia, no la de cada noche
     * @param umbral       {@code ParametrosAlojamiento.umbralEdadFacturable}
     */
    public boolean esFacturableA(LocalDate fechaEntrada, int umbral) {
        return edadA(fechaEntrada) >= umbral;
    }

    public OcupanteId id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public LocalDate fechaNacimiento() {
        return fechaNacimiento;
    }

    public Documento documento() {
        return documento;
    }

    public String nacionalidad() {
        return nacionalidad;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Ocupante otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
