package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * Entidad interna de Reserva: salida (check-out) con la fecha y hora reales y su autor; no anterior al registro
 * (SAL-05).
 */
public class Salida {

    private final SalidaId id;
    private final LocalDateTime fechaHora;
    private final UsuarioId autor;

    // SAL-05
    public Salida(SalidaId id, LocalDateTime fechaHora, UsuarioId autor) {
        if (id == null || fechaHora == null || autor == null) {
            throw new ReglaDominioException("La salida requiere id, fecha y hora, y autor");
        }
        this.id = id;
        this.fechaHora = fechaHora;
        this.autor = autor;
    }

    public SalidaId id() {
        return id;
    }

    public LocalDateTime fechaHora() {
        return fechaHora;
    }

    public UsuarioId autor() {
        return autor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Salida otra && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
