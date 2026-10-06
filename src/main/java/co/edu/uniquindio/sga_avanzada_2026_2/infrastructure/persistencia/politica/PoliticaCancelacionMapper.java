package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;

import java.math.BigDecimal;

/**
 * Convierte PoliticaCancelacion ⇄ PoliticaCancelacionJpa. Al cargar usa el constructor de reconstrucción, que vuelve
 * a validar los tramos (DEC-06).
 */
final class PoliticaCancelacionMapper {

    private PoliticaCancelacionMapper() {
    }

    static PoliticaCancelacion aDominio(PoliticaCancelacionJpa jpa) {
        return new PoliticaCancelacion(
                new PoliticaId(jpa.getId()),
                new AlojamientoId(jpa.getAlojamientoId()),
                jpa.getVersion(),
                jpa.getTramos().stream()
                        .map(t -> new TramoCancelacion(t.getAntelacionMinHoras(),
                                penalizacion(t.getBase(), t.getPorcentaje(), t.getMontoFijo())))
                        .toList(),
                penalizacion(jpa.getNoShowBase(), jpa.getNoShowPorcentaje(), jpa.getNoShowMontoFijo()));
    }

    static PoliticaCancelacionJpa aJpa(PoliticaCancelacion politica) {
        PoliticaCancelacionJpa jpa = new PoliticaCancelacionJpa();
        jpa.setId(politica.id().valor());
        jpa.setAlojamientoId(politica.alojamientoId().valor());
        jpa.setVersion(politica.version());
        Penalizacion noShow = politica.penalizacionNoShow();
        jpa.setNoShowBase(noShow.base());
        jpa.setNoShowPorcentaje(porcentaje(noShow));
        jpa.setNoShowMontoFijo(montoFijo(noShow));
        politica.tramos().forEach(t -> jpa.getTramos().add(new TramoJpa(t.antelacionMinHoras(),
                t.penalizacion().base(), porcentaje(t.penalizacion()), montoFijo(t.penalizacion()))));
        return jpa;
    }

    private static Penalizacion penalizacion(BaseRetencion base, Integer porcentaje, BigDecimal montoFijo) {
        return new Penalizacion(base, porcentaje == null ? null : new Porcentaje(porcentaje),
                montoFijo == null ? null : new Dinero(montoFijo));
    }

    private static Integer porcentaje(Penalizacion p) {
        return p.porcentaje() == null ? null : p.porcentaje().valor();
    }

    private static BigDecimal montoFijo(Penalizacion p) {
        return p.montoFijo() == null ? null : p.montoFijo().monto();
    }
}
