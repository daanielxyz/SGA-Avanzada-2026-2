package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: a quién llamar por un conflicto de canal (DEC-129). Es el titular que mandó el canal externo con la reserva
 * rechazada: sin reserva no hay titular guardado, así que el conflicto conserva su nombre y sus contactos para que el
 * administrador lo ubique. Solo eso: el documento no se guarda aquí (queda en el mensaje de la bitácora, BIT-01).
 *
 * @param telefono puede faltar si el canal no lo manda
 * @param correo   puede faltar si el canal no lo manda
 */
public record ContactoTitular(String nombre, String telefono, String correo) {

    // CONF-03 · DEC-129
    public ContactoTitular {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El contacto del conflicto requiere el nombre del titular");
        }
        nombre = nombre.trim();
        telefono = vacioANulo(telefono);
        correo = vacioANulo(correo);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
