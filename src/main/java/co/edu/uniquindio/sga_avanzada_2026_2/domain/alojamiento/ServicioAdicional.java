package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;

/**
 * Entidad interna de Alojamiento. Su valor se congela en el Cargo.
 */
public class ServicioAdicional {

    private final ServicioAdicionalId id;
    private String nombre;
    private boolean generaCargo;
    private Dinero valor; // ≥0
    private boolean activo;

    public ServicioAdicional(ServicioAdicionalId id, String nombre, boolean generaCargo, Dinero valor,
                             boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.generaCargo = generaCargo;
        this.valor = valor;
        this.activo = activo;
    }
}
