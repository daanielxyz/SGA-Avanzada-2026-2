package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.TipoPago;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila de {@code folio_pago}. Guarda el nombre del medio, así deshabilitarlo después no altera el histórico (MPAG-03).
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PagoJpa {

    private String id;
    private String medio;
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    private TipoPago tipo;

    private LocalDate fecha;
    private String reversaDe;
}
