package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;

/**
 * Usuario del personal interno, con su tabla. No es del dominio (USU-07): el negocio solo conoce su id como autor. El
 * huésped no está aquí: su cuenta es su titular (DEC-68).
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

    private String telefono;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;

    private String numeroDocumento;
    private LocalDate fechaNacimiento;
    private boolean eliminado;
    /** Va en cada token suyo; sube al cambiar la contraseña y deja sin valor los tokens anteriores (DEC-71). */
    private int versionSesiones;

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
     * Crea un usuario con contraseña, habilitado para iniciar sesión, con exactamente un rol (ROL-02). Lo usa el
     * arranque para el primer administrador (DEC-67).
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

    /**
     * Crea un usuario interno sin contraseña: la fija él con el código de activación (USU-03 · DEC-71).
     *
     * @throws ReglaDominioException si el rol no es interno: los huéspedes no son usuarios (DEC-68)
     */
    public static Usuario crearInterno(UsuarioId id, String alojamientoId, Correo correo, Rol rol,
                                       DatosPersonales datos) {
        exigirInterno(rol);
        Usuario usuario = new Usuario(id.valor(), alojamientoId, null, correo.valor(), null, rol);
        usuario.actualizarDatos(datos);
        return usuario;
    }

    /** Los datos que pide USU-01, salvo el correo y la contraseña. */
    public record DatosPersonales(String nombre, String telefono, TipoDocumento tipoDocumento,
                                  String numeroDocumento, LocalDate fechaNacimiento) {
    }

    /**
     * Cambia los datos personales. El correo no se edita después del registro (USU-01).
     *
     * @throws IllegalArgumentException si falta el nombre
     */
    public void actualizarDatos(DatosPersonales datos) {
        if (datos.nombre() == null || datos.nombre().isBlank()) {
            throw new IllegalArgumentException("El usuario necesita nombre");
        }
        nombre = datos.nombre().trim();
        telefono = datos.telefono();
        tipoDocumento = datos.tipoDocumento();
        numeroDocumento = datos.numeroDocumento();
        fechaNacimiento = datos.fechaNacimiento();
    }

    /**
     * Cambia el rol por otro interno; el usuario sigue siendo el mismo (ROL-03 · ROL-05). Sus sesiones abiertas dejan
     * de valer porque su token lleva el rol anterior (DEC-71).
     *
     * @throws ReglaDominioException si el rol nuevo no es interno
     */
    public void cambiarRol(Rol nuevo) {
        exigirInterno(nuevo);
        rol = nuevo;
    }

    /**
     * Fija una contraseña nueva y deja sin efecto las sesiones que ya estaban abiertas (USU-02 · DEC-71).
     *
     * @param contrasenaCifrada resumen de la contraseña, nunca el texto plano
     */
    public void fijarContrasena(String contrasenaCifrada) {
        contrasena = contrasenaCifrada;
        versionSesiones++;
    }

    /** Eliminación lógica: deja de entrar y sus sesiones se cortan (USU-05 · DEC-71). */
    public void eliminar() {
        eliminado = true;
    }

    private static void exigirInterno(Rol rol) {
        if (rol == null || rol == Rol.HUESPED) {
            throw new ReglaDominioException("Un usuario del sistema solo puede tener un rol interno");
        }
    }

    /** Un usuario eliminado o que todavía no fijó su contraseña no inicia sesión (USU-05 · CU-02 · DEC-71). */
    public boolean puedeIniciarSesion() {
        return !eliminado && contrasena != null;
    }

    /**
     * Si un token suyo sigue valiendo: el usuario no fue eliminado, tiene el mismo rol que el token y no cambió la
     * contraseña después de emitirlo (DEC-71).
     *
     * @param versionDelToken {@link #versionSesiones()} cuando se emitió; nula en un token sin ella
     */
    public boolean aceptaSesion(String rolDelToken, Long versionDelToken) {
        return puedeIniciarSesion() && rol.name().equals(rolDelToken) && versionDelToken != null
                && versionDelToken == versionSesiones;
    }

    public int versionSesiones() {
        return versionSesiones;
    }

    public boolean estaActivado() {
        return contrasena != null;
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

    public String telefono() {
        return telefono;
    }

    public TipoDocumento tipoDocumento() {
        return tipoDocumento;
    }

    public String numeroDocumento() {
        return numeroDocumento;
    }

    public LocalDate fechaNacimiento() {
        return fechaNacimiento;
    }

    public boolean eliminado() {
        return eliminado;
    }
}
