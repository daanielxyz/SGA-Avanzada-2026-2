package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import jakarta.validation.constraints.Size;

/**
 * Cuerpo opcional de {@code PUT /api/apartamentos/{codigo}/declarar-fuera-de-servicio}.
 *
 * @param motivo por qué sale de operación, por ejemplo «Fuga de agua en el baño». Lo ven recepción y el personal en el
 *               tablero y en la preparación mientras siga fuera de servicio (DEC-113); en blanco cuenta como no dado
 */
public record DeclararFueraDeServicioRequest(
        @Size(max = 500, message = "admite máximo 500 caracteres")
        String motivo) {
}
