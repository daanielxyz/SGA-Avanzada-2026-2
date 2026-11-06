package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.FijarLimiteMascotasCommand;
import jakarta.validation.constraints.Min;

/**
 * Cuerpo de {@code PUT /api/apartamentos/{codigo}/limite-mascotas} (DEC-93).
 *
 * @param limiteMascotas las que admite el apartamento por reserva, 0 si no recibe; vacío o ausente, vuelve al límite
 *                       del alojamiento
 */
public record LimiteMascotasRequest(
        @Min(value = 0, message = "no puede ser negativo")
        Integer limiteMascotas) {

    FijarLimiteMascotasCommand aCommand(String codigo) {
        return new FijarLimiteMascotasCommand(codigo, limiteMascotas);
    }
}
