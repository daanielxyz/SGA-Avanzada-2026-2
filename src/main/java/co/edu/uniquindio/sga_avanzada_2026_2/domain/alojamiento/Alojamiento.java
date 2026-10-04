package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;

import java.util.List;

/**
 * Raíz del agregado Alojamiento.
 */
public class Alojamiento {

    private final AlojamientoId id;
    private String nombre;
    private String descripcion;
    private String ciudad;
    private String direccion;
    private Ubicacion ubicacion;
    private String normas;
    private ParametrosAlojamiento parametros;
    private List<ServicioAdicional> serviciosAdicionales; // ≥1
    private List<MedioPago> mediosPago;                   // ≥2, catálogo habilitado

    public Alojamiento(AlojamientoId id, String nombre, String descripcion, String ciudad, String direccion,
                       Ubicacion ubicacion, String normas, ParametrosAlojamiento parametros,
                       List<ServicioAdicional> serviciosAdicionales, List<MedioPago> mediosPago) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.ubicacion = ubicacion;
        this.normas = normas;
        this.parametros = parametros;
        this.serviciosAdicionales = List.copyOf(serviciosAdicionales);
        this.mediosPago = List.copyOf(mediosPago);
    }
}
