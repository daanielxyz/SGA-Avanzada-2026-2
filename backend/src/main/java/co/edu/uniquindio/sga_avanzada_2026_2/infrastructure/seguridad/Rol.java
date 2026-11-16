package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

/**
 * Conjunto cerrado de roles (ROL-01). Los internos son los tres últimos (USU-03).
 */
public enum Rol {
    HUESPED("Huésped"),
    ADMINISTRADOR("Administración"),
    RECEPCIONISTA("Recepción"),
    PERSONAL_SERVICIO("Personal de servicio");

    private final String texto;

    Rol(String texto) {
        this.texto = texto;
    }

    /** Cómo se le dice a una persona, en un correo o un mensaje: «Recepción», no RECEPCIONISTA (DEC-135). */
    public String texto() {
        return texto;
    }

    /** Nombre con el que Spring Security conoce el rol. */
    public String autoridad() {
        return "ROLE_" + name();
    }
}
