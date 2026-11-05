package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

/**
 * VO: lo que una reserva suma a su estancia por parqueadero y mascotas, congelado al crearla (RP-02 · RP-03 · DEC-89).
 * Entra en el anticipo, pero no en la retención por cancelación ni por no-show: esa sale solo de la estancia.
 */
public record Extras(Dinero parqueadero, Dinero mascotas) {

    public static final Extras NINGUNO = new Extras(Dinero.CERO, Dinero.CERO);

    // DEC-89
    public Extras {
        if (parqueadero == null || mascotas == null) {
            throw new ReglaDominioException("Faltan los valores de parqueadero y mascotas");
        }
    }

    public Dinero total() {
        return parqueadero.sumar(mascotas);
    }
}
