package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.folio;

import jakarta.validation.constraints.Size;

/**
 * Cuerpo opcional de {@code PUT /api/reservas/{codigo}/folio/cerrar}.
 *
 * @param motivoAutorizacion solo para cerrar con saldo distinto de cero (RN-17). Que solo el administrador pueda
 *                           enviarlo se controla en B3 (DEC-61)
 */
public record CerrarFolioRequest(
        @Size(max = 500, message = "admite máximo 500 caracteres")
        String motivoAutorizacion) {
}
