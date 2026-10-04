package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;

import java.time.LocalDate;

/**
 * Entidad interna de Reserva. La edad se calcula, no se guarda.
 */
public class Ocupante {

    private final OcupanteId id;
    private final String nombre;
    private final LocalDate fechaNacimiento; // no futura
    private final Documento documento;       // opcional
    private final String nacionalidad;       // opcional

    public Ocupante(OcupanteId id, String nombre, LocalDate fechaNacimiento, Documento documento,
                    String nacionalidad) {
        this.id = id;
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
        this.documento = documento;
        this.nacionalidad = nacionalidad;
    }
}
