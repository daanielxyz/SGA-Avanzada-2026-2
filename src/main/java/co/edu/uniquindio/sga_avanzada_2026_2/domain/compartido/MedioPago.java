package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

/**
 * VO: medio de pago. Se valida contra el catálogo habilitado del Alojamiento, no aquí.
 */
public record MedioPago(String nombre) {

    public MedioPago {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del medio de pago es obligatorio");
        }
        nombre = nombre.trim().toUpperCase();
    }
}
