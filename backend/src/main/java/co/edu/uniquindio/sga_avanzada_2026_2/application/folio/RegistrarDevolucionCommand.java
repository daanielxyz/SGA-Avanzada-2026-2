package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entrada de {@link RegistrarDevolucionUseCase}: el folio se identifica por su reserva (FOL-02).
 *
 * @param medio nombre del medio por el que se devuelve, que debe estar habilitado en el alojamiento (MPAG-02)
 * @param fecha cuándo se devolvió el dinero; no puede ser futura (PAG-07)
 * @param autor quien registra la devolución (DEC-100)
 */
public record RegistrarDevolucionCommand(String codigoReserva, String medio, BigDecimal monto, LocalDate fecha,
                                         String autor) {
}
