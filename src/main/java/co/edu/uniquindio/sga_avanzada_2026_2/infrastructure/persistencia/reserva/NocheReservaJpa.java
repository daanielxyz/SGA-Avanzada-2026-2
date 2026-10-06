package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila de {@code reserva_desglose_noche}: una línea del desglose congelado (DEC-05).
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NocheReservaJpa {

    private LocalDate noche;
    private String temporadaId;
    private BigDecimal tarifa;
    private int ocupantesFacturables;
    private BigDecimal subtotal;
}
