package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

/**
 * Usuario interno del sistema (fuera del dominio).
 */
public class Usuario {

    private final UsuarioId id;
    private String nombre;
    private String correo;
    private Rol rol;
    private boolean activo;

    public Usuario(UsuarioId id, String nombre, String correo, Rol rol, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.activo = activo;
    }
}
