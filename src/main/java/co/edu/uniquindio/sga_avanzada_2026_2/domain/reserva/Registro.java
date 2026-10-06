package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * Entidad interna de Reserva: registro de llegada (check-in) con la fecha y hora reales y su autor (REG-05). Uno
 * equivocado se anula, no se borra (REG-06).
 */
public class Registro {

    private final RegistroId id;
    private final LocalDateTime fechaHora;
    private final UsuarioId autor;
    private final boolean anulado; // TODO(equipo): la anulación (REG-06) se implementa con la llegada en A6

    // REG-05
    public Registro(RegistroId id, LocalDateTime fechaHora, UsuarioId autor, boolean anulado) {
        if (id == null || fechaHora == null || autor == null) {
            throw new ReglaDominioException("El registro requiere id, fecha y hora, y autor");
        }
        this.id = id;
        this.fechaHora = fechaHora;
        this.autor = autor;
        this.anulado = anulado;
    }

    public RegistroId id() {
        return id;
    }

    public LocalDateTime fechaHora() {
        return fechaHora;
    }

    public UsuarioId autor() {
        return autor;
    }

    public boolean anulado() {
        return anulado;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Registro otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
