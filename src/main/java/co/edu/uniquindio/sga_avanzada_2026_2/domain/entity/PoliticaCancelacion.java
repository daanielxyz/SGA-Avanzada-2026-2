package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TramoCancelacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Entidad Raíz: Agregado PoliticaCancelacion.
 */
public class PoliticaCancelacion {

    private final PoliticaId id;
    private final int version;
    private final List<TramoCancelacion> tramos;
    private final Porcentaje retencionNoShow;
    private boolean vigente;

    private PoliticaCancelacion(PoliticaId id, int version, List<TramoCancelacion> tramos,
                                Porcentaje retencionNoShow, boolean vigente) {
        this.id = id;
        this.version = version;
        this.tramos = new ArrayList<>(tramos);
        this.retencionNoShow = retencionNoShow;
        this.vigente = vigente;
    }

    public static PoliticaCancelacion crear(PoliticaId id, int version, List<TramoCancelacion> tramos,
                                            Porcentaje retencionNoShow, boolean vigente) {
        Objects.requireNonNull(id, "El id de la política no puede ser nulo");
        if (version < 1) {
            throw new ReglaDominioException("La versión de la política debe ser mayor o igual a 1");
        }
        Objects.requireNonNull(tramos, "La lista de tramos no puede ser nula");
        if (tramos.size() < 2) {
            throw new ReglaDominioException("La política de cancelación debe contener al menos 2 tramos");
        }
        Objects.requireNonNull(retencionNoShow, "El porcentaje de retención por no-show no puede ser nulo");

        return new PoliticaCancelacion(id, version, tramos, retencionNoShow, vigente);
    }

    public Porcentaje retencionPara(int diasAntelacion) {
        if (diasAntelacion < 0) {
            return retencionNoShow;
        }
        // Buscar el tramo correspondiente ordenado por antelacionMinDias descendente
        return tramos.stream()
                .sorted(Comparator.comparingInt(TramoCancelacion::antelacionMinDias).reversed())
                .filter(t -> diasAntelacion >= t.antelacionMinDias())
                .map(TramoCancelacion::retencion)
                .findFirst()
                .orElse(retencionNoShow);
    }

    public PoliticaCancelacion nuevaVersion(List<TramoCancelacion> nuevosTramos) {
        Objects.requireNonNull(nuevosTramos, "Los nuevos tramos no pueden ser nulos");
        if (nuevosTramos.size() < 2) {
            throw new ReglaDominioException("La nueva versión debe contener al menos 2 tramos");
        }
        return new PoliticaCancelacion(PoliticaId.nuevo(), this.version + 1, nuevosTramos,
                this.retencionNoShow, true);
    }

    public PoliticaId getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public List<TramoCancelacion> getTramos() {
        return Collections.unmodifiableList(tramos);
    }

    public Porcentaje getRetencionNoShow() {
        return retencionNoShow;
    }

    public boolean isVigente() {
        return vigente;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PoliticaCancelacion that = (PoliticaCancelacion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
