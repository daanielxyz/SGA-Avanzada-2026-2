package co.edu.uniquindio.sga_avanzada_2026_2.domain.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.util.Objects;

/**
 * VO: cuánto se retiene: un porcentaje de la base o un monto fijo que nunca la supera (POL-03 · DEC-41). La
 * devolución es el complemento de lo retenido.
 *
 * @param porcentaje solo si es porcentual
 * @param montoFijo  solo si es de monto fijo
 */
public record Penalizacion(BaseRetencion base, Porcentaje porcentaje, Dinero montoFijo) {

    // POL-03 · DEC-41
    public Penalizacion {
        if (base == null) {
            throw new ReglaDominioException("La penalización requiere la base sobre la que se calcula");
        }
        if ((porcentaje == null) == (montoFijo == null)) {
            throw new ReglaDominioException("La penalización es un porcentaje o un monto fijo, no ambos ni ninguno");
        }
    }

    public static Penalizacion porcentaje(BaseRetencion base, Porcentaje porcentaje) {
        return new Penalizacion(base, porcentaje, null);
    }

    public static Penalizacion montoFijo(BaseRetencion base, Dinero monto) {
        return new Penalizacion(base, null, monto);
    }

    /**
     * Monto retenido según la base elegida: el porcentaje de la base, o el monto fijo limitado a la base (POL-03 ·
     * DEC-41). Se redondea una sola vez (DIN-02).
     *
     * @param valorTotal      valor congelado de la reserva
     * @param pagado          pagos netos del folio
     * @param anticipoExigido valor total × porcentaje de anticipo del alojamiento
     * @return nunca mayor que la base
     */
    public Dinero calcular(Dinero valorTotal, Dinero pagado, Dinero anticipoExigido) {
        Dinero valorBase = switch (base) {
            case VALOR_TOTAL -> Objects.requireNonNull(valorTotal, "valorTotal");
            case PAGADO -> Objects.requireNonNull(pagado, "pagado");
            case ANTICIPO_EXIGIDO -> Objects.requireNonNull(anticipoExigido, "anticipoExigido");
        };
        if (porcentaje != null) {
            return valorBase.porcentaje(porcentaje);
        }
        return montoFijo.esMenorQue(valorBase) ? montoFijo : valorBase;
    }
}
