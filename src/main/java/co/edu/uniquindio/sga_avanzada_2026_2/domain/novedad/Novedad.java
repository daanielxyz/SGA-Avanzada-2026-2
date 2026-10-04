package co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDate;

/**
 * Raíz del agregado Novedad. No se elimina, se cierra.
 */
public class Novedad {

    private final NovedadId id;
    private final ApartamentoId apartamentoId;
    private final LocalDate fecha;
    private final UsuarioId autor;
    private final String descripcion;
    private final GravedadNovedad gravedad;
    private EstadoNovedad estado;

    public Novedad(NovedadId id, ApartamentoId apartamentoId, LocalDate fecha, UsuarioId autor,
                   String descripcion, GravedadNovedad gravedad, EstadoNovedad estado) {
        this.id = id;
        this.apartamentoId = apartamentoId;
        this.fecha = fecha;
        this.autor = autor;
        this.descripcion = descripcion;
        this.gravedad = gravedad;
        this.estado = estado;
    }
}
