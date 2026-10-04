package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad interna de Apartamento: bloqueo operativo [fechaInicio, fechaFin) (BLO-02). No se borra al
 * levantarse, queda registrado con su vigencia (BLO-06). Solo cambia a través de su Apartamento.
 */
public class Bloqueo {

    private final BloqueoId id;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin; // exclusiva, como la Estancia
    private final String motivo;
    private boolean vigente;

    // BLO-02
    public Bloqueo(BloqueoId id, LocalDate fechaInicio, LocalDate fechaFin, String motivo, boolean vigente) {
        if (id == null || fechaInicio == null || fechaFin == null) {
            throw new ReglaDominioException("El bloqueo requiere id, fecha de inicio y fecha de fin");
        }
        if (!fechaFin.isAfter(fechaInicio)) {
            throw new ReglaDominioException("El fin del bloqueo debe ser posterior al inicio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("El motivo del bloqueo es obligatorio");
        }
        this.id = id;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.motivo = motivo.trim();
        this.vigente = vigente;
    }

    /**
     * Indica si el bloqueo impide vender la noche: debe estar vigente y la noche dentro de
     * [fechaInicio, fechaFin) (RN-07 · BLO-03).
     *
     * @param noche noche a consultar
     * @return {@code true} si la noche queda bloqueada
     */
    public boolean cubre(Noche noche) {
        Objects.requireNonNull(noche, "noche");
        LocalDate fecha = noche.fecha();
        return vigente && !fecha.isBefore(fechaInicio) && fecha.isBefore(fechaFin);
    }

    /**
     * Levanta el bloqueo: sus noches quedan libres de inmediato y el registro se conserva (BLO-06 · APA-12).
     * Solo lo invoca {@link Apartamento#levantarBloqueo}.
     *
     * @throws ReglaDominioException si ya estaba levantado
     */
    void levantar() {
        if (!vigente) {
            throw new ReglaDominioException("El bloqueo " + id.valor() + " ya fue levantado");
        }
        vigente = false;
    }

    public BloqueoId id() {
        return id;
    }

    public LocalDate fechaInicio() {
        return fechaInicio;
    }

    public LocalDate fechaFin() {
        return fechaFin;
    }

    public String motivo() {
        return motivo;
    }

    public boolean vigente() {
        return vigente;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bloqueo otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
