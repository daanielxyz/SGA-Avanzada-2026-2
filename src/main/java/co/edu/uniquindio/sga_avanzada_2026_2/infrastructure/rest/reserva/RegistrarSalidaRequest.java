package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import jakarta.validation.constraints.Size;

/**
 * Cuerpo opcional de {@code PUT /api/reservas/{codigo}/registrar-salida}.
 *
 * @param motivoAutorizacion solo para salir con saldo distinto de cero (CU-38 · RN-17). Solo el administrador
 *                           puede enviarlo (AUTC-01 · DEC-67)
 */
public record RegistrarSalidaRequest(
        @Size(max = 500, message = "admite máximo 500 caracteres")
        String motivoAutorizacion) {
}
