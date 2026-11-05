package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;

/**
 * Cómo cobra el parqueadero cada alojamiento (RP-02 · DEC-89).
 */
public enum CobroParqueadero {

    /** El precio se cobra por cada noche de la estancia. */
    POR_NOCHE {
        @Override
        public Dinero cargoDeUnVehiculo(Dinero precio, int noches) {
            return precio.multiplicar(noches);
        }
    },

    /** El precio se cobra una vez, sin importar las noches. */
    POR_ESTADIA {
        @Override
        public Dinero cargoDeUnVehiculo(Dinero precio, int noches) {
            return precio;
        }
    };

    /**
     * @param precio {@code ParametrosAlojamiento.cargoCarro} o {@code cargoMoto}
     */
    public abstract Dinero cargoDeUnVehiculo(Dinero precio, int noches);
}
