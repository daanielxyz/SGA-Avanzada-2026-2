package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * Entidad interna de Reserva: registro de llegada (check-in). Uno equivocado se anula, no se borra.
 */
public class Registro {

    private final RegistroId id;
    private final LocalDateTime fechaHora;
    private final UsuarioId autor;
    private boolean anulado;

    public Registro(RegistroId id, LocalDateTime fechaHora, UsuarioId autor, boolean anulado) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.autor = autor;
        this.anulado = anulado;
    }
}
