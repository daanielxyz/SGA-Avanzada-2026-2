package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;

import java.util.List;

/**
 * Raíz del agregado Apartamento.
 */
public class Apartamento {

    private final ApartamentoId codigo;
    private final AlojamientoId alojamientoId;
    private String nombre;
    private String descripcion;
    private Capacidad capacidad;
    private Dormitorio dormitorios;
    private EstadoOperativo estadoOperativo;
    private List<Imagen> imagenes;               // 1..10, una principal
    private List<Caracteristica> caracteristicas; // ≥1
    private List<Bloqueo> bloqueos;
    private boolean activo;

    public Apartamento(ApartamentoId codigo, AlojamientoId alojamientoId, String nombre, String descripcion,
                       Capacidad capacidad, Dormitorio dormitorios, EstadoOperativo estadoOperativo,
                       List<Imagen> imagenes, List<Caracteristica> caracteristicas, List<Bloqueo> bloqueos,
                       boolean activo) {
        this.codigo = codigo;
        this.alojamientoId = alojamientoId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.capacidad = capacidad;
        this.dormitorios = dormitorios;
        this.estadoOperativo = estadoOperativo;
        this.imagenes = List.copyOf(imagenes);
        this.caracteristicas = List.copyOf(caracteristicas);
        this.bloqueos = List.copyOf(bloqueos);
        this.activo = activo;
    }
}
