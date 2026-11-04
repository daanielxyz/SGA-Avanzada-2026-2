package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;

/**
 * Cuánto se paga en línea al reservar desde el portal; lo decide cada alojamiento (DEC-69).
 */
public enum ModoPagoEnLinea {

    /** El porcentaje de anticipo del alojamiento sobre el valor congelado (RES-15). */
    ANTICIPO {
        @Override
        public Dinero exigido(Dinero valorTotal, Porcentaje anticipo) {
            return valorTotal.porcentaje(anticipo);
        }
    },

    /** Toda la estancia. */
    TOTAL {
        @Override
        public Dinero exigido(Dinero valorTotal, Porcentaje anticipo) {
            return valorTotal;
        }
    };

    /**
     * Lo que hay que haber pagado en línea, antes de descontar lo que ya esté pagado.
     *
     * @param anticipo {@code ParametrosAlojamiento.anticipo}
     */
    public abstract Dinero exigido(Dinero valorTotal, Porcentaje anticipo);
}
