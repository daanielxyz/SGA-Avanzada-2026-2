package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import java.time.LocalDate;

/**
 * Raíz del agregado Temporada. No se solapan entre sí; la base cubre lo no asignado y es obligatoria.
 */
public class Temporada {

    private final TemporadaId id;
    private String nombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin; // inclusiva; null si es la base
    private final boolean esBase;
    private int estanciaMinimaNoches;

    public Temporada(TemporadaId id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, boolean esBase,
                     int estanciaMinimaNoches) {
        this.id = id;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.esBase = esBase;
        this.estanciaMinimaNoches = estanciaMinimaNoches;
    }
}
