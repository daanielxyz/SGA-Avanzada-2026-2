package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

/**
 * Política de contraseñas (USU-02): mínimo 8 caracteres, con mayúsculas y números.
 */
public final class PoliticaContrasena {

    public static final String DESCRIPCION = "debe tener mínimo 8 caracteres, con mayúsculas y números";

    private PoliticaContrasena() {
    }

    public static boolean laCumple(String contrasena) {
        return contrasena != null && contrasena.length() >= 8
                && contrasena.chars().anyMatch(Character::isUpperCase)
                && contrasena.chars().anyMatch(Character::isDigit);
    }
}
