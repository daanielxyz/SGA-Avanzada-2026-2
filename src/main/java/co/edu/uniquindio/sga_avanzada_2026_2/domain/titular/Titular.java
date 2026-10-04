package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;

/**
 * Raíz del agregado Titular: responsable de la reserva.
 */
public class Titular {

    private final TitularId id;
    private String nombre;
    private Documento documento;
    private Correo correo;
    private String telefono;

    public Titular(TitularId id, String nombre, Documento documento, Correo correo, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.documento = documento;
        this.correo = correo;
        this.telefono = telefono;
    }
}
