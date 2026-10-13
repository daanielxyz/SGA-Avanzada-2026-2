package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Usuario del módulo de acceso, con su tabla. No es del dominio (USU-07): el negocio solo conoce su id como autor.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private String id;

    private String alojamientoId;
    private String nombre;
    private String correo;
    private String contrasena;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    private boolean eliminado;

    @Version
    private Long version;

    protected Usuario() {
    }

    private Usuario(String id, String alojamientoId, String nombre, String correo, String contrasena, Rol rol) {
        this.id = id;
        this.alojamientoId = alojamientoId;
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
        this.rol = rol;
    }

    /**
     * Crea un usuario habilitado para iniciar sesión, con exactamente un rol (ROL-02).
     *
     * @param correo            se guarda normalizado; identifica al usuario (USU-01 · CORR-01)
     * @param contrasenaCifrada resumen de la contraseña, nunca el texto plano (USU-02)
     * @throws IllegalArgumentException si falta el nombre, la contraseña o el rol
     */
    public static Usuario crear(UsuarioId id, String alojamientoId, String nombre, Correo correo,
                                String contrasenaCifrada, Rol rol) {
        if (nombre == null || nombre.isBlank() || contrasenaCifrada == null || contrasenaCifrada.isBlank()
                || rol == null) {
            throw new IllegalArgumentException("El usuario necesita nombre, contraseña y rol");
        }
        return new Usuario(id.valor(), alojamientoId, nombre.trim(), correo.valor(), contrasenaCifrada, rol);
    }

    /** Un usuario eliminado lógicamente no inicia sesión (USU-05 · CU-02). */
    public boolean puedeIniciarSesion() {
        return !eliminado;
    }

    public String id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String correo() {
        return correo;
    }

    public String contrasena() {
        return contrasena;
    }

    public Rol rol() {
        return rol;
    }
}
