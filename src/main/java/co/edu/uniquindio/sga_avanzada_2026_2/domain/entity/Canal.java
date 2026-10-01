package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.CanalOrigen;

import java.util.Objects;

/**
 * Entidad Raíz: Agregado Canal (canal de distribución / venta).
 */
public class Canal {

    private final CanalId id;
    private final String nombre;
    private final CanalOrigen tipo;
    private final String credencial;
    private boolean activo;

    private Canal(CanalId id, String nombre, CanalOrigen tipo, String credencial, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.credencial = credencial;
        this.activo = activo;
    }

    public static Canal crear(CanalId id, String nombre, CanalOrigen tipo, String credencial, boolean activo) {
        Objects.requireNonNull(id, "El id del canal no puede ser nulo");
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del canal no puede estar vacío");
        }
        Objects.requireNonNull(tipo, "El tipo de canal origen no puede ser nulo");
        if (credencial == null || credencial.isBlank()) {
            throw new ReglaDominioException("La credencial del canal no puede estar vacía");
        }

        return new Canal(id, nombre.trim(), tipo, credencial.trim(), activo);
    }

    public void desactivar() {
        this.activo = false;
    }

    public CanalId getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public CanalOrigen getTipo() {
        return tipo;
    }

    public String getCredencial() {
        return credencial;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Canal canal = (Canal) o;
        return Objects.equals(id, canal.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
