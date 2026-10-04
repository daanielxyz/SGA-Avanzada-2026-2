package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import java.time.LocalDate;

/**
 * Entidad interna de Apartamento: bloqueo operativo [fechaInicio, fechaFin).
 */
public class Bloqueo {

    private final BloqueoId id;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin; // exclusiva, como la Estancia
    private final String motivo;
    private boolean vigente;

    public Bloqueo(BloqueoId id, LocalDate fechaInicio, LocalDate fechaFin, String motivo, boolean vigente) {
        this.id = id;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.motivo = motivo;
        this.vigente = vigente;
    }
}
