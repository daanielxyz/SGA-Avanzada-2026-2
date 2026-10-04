package co.edu.uniquindio.sga_avanzada_2026_2.domain.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;

import java.util.List;

/**
 * Raíz del agregado PoliticaCancelacion (una instancia por versión).
 */
// TODO(equipo): tramos restantes de la política de cancelación por definir.
public class PoliticaCancelacion {

    private final PoliticaId id;
    private final AlojamientoId alojamientoId;
    private final int version;
    private final List<TramoCancelacion> tramos; // ≥2
    private final Porcentaje retencionNoShow;
    private boolean vigente;

    public PoliticaCancelacion(PoliticaId id, AlojamientoId alojamientoId, int version,
                               List<TramoCancelacion> tramos, Porcentaje retencionNoShow, boolean vigente) {
        this.id = id;
        this.alojamientoId = alojamientoId;
        this.version = version;
        this.tramos = List.copyOf(tramos);
        this.retencionNoShow = retencionNoShow;
        this.vigente = vigente;
    }
}
