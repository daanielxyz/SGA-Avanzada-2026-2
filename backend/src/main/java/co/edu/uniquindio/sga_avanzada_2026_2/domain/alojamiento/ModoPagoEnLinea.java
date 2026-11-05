package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;

/**
 * Cuánto se paga en línea al reservar desde el portal; lo decide cada alojamiento (DEC-69).
 */
public enum ModoPagoEnLinea {

    /** El porcentaje de anticipo del alojamiento sobre el valor congelado con sus extras (RES-15 · DEC-89). */
    ANTICIPO {
        @Override
        public Dinero exigido(Dinero valorConExtras, Porcentaje anticipo) {
            return valorConExtras.porcentaje(anticipo);
        }
    },

    /** Toda la estancia y sus extras. */
    TOTAL {
        @Override
        public Dinero exigido(Dinero valorConExtras, Porcentaje anticipo) {
            return valorConExtras;
        }
    };

    /**
     * Lo que hay que haber pagado en línea, antes de descontar lo que ya esté pagado.
     *
     * @param anticipo {@code ParametrosAlojamiento.anticipo}
     */
    public abstract Dinero exigido(Dinero valorConExtras, Porcentaje anticipo);
}
