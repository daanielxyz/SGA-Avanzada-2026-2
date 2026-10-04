package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * Entidad interna de Reserva: salida (check-out), no anterior al registro.
 */
public class Salida {

    private final SalidaId id;
    private final LocalDateTime fechaHora;
    private final UsuarioId autor;

    public Salida(SalidaId id, LocalDateTime fechaHora, UsuarioId autor) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.autor = autor;
    }
}
