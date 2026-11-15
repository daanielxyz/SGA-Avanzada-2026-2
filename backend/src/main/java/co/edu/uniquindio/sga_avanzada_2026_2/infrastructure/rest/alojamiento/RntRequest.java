package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de {@code PUT /api/alojamiento/rnt} (DEC-130). Vacío o null borra el RNT; que sean solo dígitos lo valida
 * también el dominio.
 */
public record RntRequest(
        @Pattern(regexp = "^$|^[0-9]{1,20}$", message = "son solo números, hasta 20")
        String rnt) {
}
