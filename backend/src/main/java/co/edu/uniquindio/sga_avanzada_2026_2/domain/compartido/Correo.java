package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

/**
 * VO: correo electrónico con formato válido, normalizado en minúsculas.
 */
public record Correo(String valor) {

    public Correo {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("El correo es obligatorio");
        }
        valor = valor.trim().toLowerCase();
        if (!valor.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ReglaDominioException("Correo con formato inválido: " + valor);
        }
    }
}
