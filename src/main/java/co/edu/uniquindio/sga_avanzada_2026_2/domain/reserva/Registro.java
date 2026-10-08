package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * Entidad interna de Reserva: registro de llegada (check-in) con la fecha y hora reales y su autor (REG-05). Uno
 * equivocado se anula y se reemplaza por una corrección, nunca se borra (REG-06 · DEC-45). Solo cambia a través de su
 * Reserva (DEC-23).
 */
public class Registro {

    private final RegistroId id;
    private final LocalDateTime fechaHora;
    private final UsuarioId autor;
    private boolean anulado;
    private final String motivoCorreccion; // solo si este registro corrige a uno anulado

    // REG-05 · REG-06
    public Registro(RegistroId id, LocalDateTime fechaHora, UsuarioId autor, boolean anulado,
                    String motivoCorreccion) {
        if (id == null || fechaHora == null || autor == null) {
            throw new ReglaDominioException("El registro requiere id, fecha y hora, y autor");
        }
        if (motivoCorreccion != null && motivoCorreccion.isBlank()) {
            throw new ReglaDominioException("El motivo de la corrección no puede estar vacío");
        }
        this.id = id;
        this.fechaHora = fechaHora;
        this.autor = autor;
        this.anulado = anulado;
        this.motivoCorreccion = motivoCorreccion == null ? null : motivoCorreccion.trim();
    }

    /**
     * Anula el registro: se conserva como historial y deja de ser el vigente (REG-06). Solo lo invoca
     * {@link Reserva#corregirRegistro}.
     *
     * @throws ReglaDominioException si ya estaba anulado
     */
    void anular() {
        if (anulado) {
            throw new ReglaDominioException("El registro " + id.valor() + " ya está anulado");
        }
        anulado = true;
    }

    public boolean esCorreccion() {
        return motivoCorreccion != null;
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

    public String motivoCorreccion() {
        return motivoCorreccion;
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
