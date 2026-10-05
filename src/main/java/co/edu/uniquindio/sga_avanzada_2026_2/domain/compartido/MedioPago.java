package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

/**
 * VO: forma de liquidar un pago, normalizada en mayúsculas. No es enum: el catálogo lo configura cada alojamiento
 * (MPAG-06) y es él quien decide si está habilitado (MPAG-02).
 */
public record MedioPago(String nombre) {

    public MedioPago {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del medio de pago es obligatorio");
        }
        nombre = nombre.trim().toUpperCase();
    }
}
