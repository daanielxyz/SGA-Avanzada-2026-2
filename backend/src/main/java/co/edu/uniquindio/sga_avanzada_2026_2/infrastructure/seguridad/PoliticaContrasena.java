package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

/**
 * Política de contraseñas del personal interno (USU-02): mínimo 8 caracteres, con mayúsculas y números. Al huésped no
 * se le aplica: entra con PIN (DEC-68).
 */
public final class PoliticaContrasena {

    public static final String DESCRIPCION = "debe tener mínimo 8 caracteres, con mayúsculas y números";

    /** La misma política como expresión regular, para validar el formato en el borde (400). */
    public static final String PATRON = "^(?=.*\\p{Lu})(?=.*\\d).{8,}$";

    private PoliticaContrasena() {
    }

    public static boolean laCumple(String contrasena) {
        return contrasena != null && contrasena.length() >= 8
                && contrasena.chars().anyMatch(Character::isUpperCase)
                && contrasena.chars().anyMatch(Character::isDigit);
    }
}
