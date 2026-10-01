package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.Set;

/**
 * Objeto de valor: Medio de pago habilitado en el catálogo del sistema.
 */
public record MedioPago(String nombre) {

    public static final Set<String> CATALOGO_HABILITADO = Set.of(
            "EFECTIVO",
            "TARJETA_CREDITO",
            "TARJETA_DEBITO",
            "TRANSFERENCIA",
            "PSE"
    );

    public MedioPago {
        Objects.requireNonNull(nombre, "El nombre del medio de pago no puede ser nulo");
        String normalizado = nombre.trim().toUpperCase();
        if (!CATALOGO_HABILITADO.contains(normalizado)) {
            throw new IllegalArgumentException("Medio de pago no habilitado en catálogo: " + nombre);
        }
        nombre = normalizado;
    }
}
