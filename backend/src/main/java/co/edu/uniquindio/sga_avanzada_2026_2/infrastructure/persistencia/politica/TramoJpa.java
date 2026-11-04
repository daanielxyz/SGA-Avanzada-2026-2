package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Fila de {@code politica_tramo}: el VO TramoCancelacion con su penalización aplanada.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TramoJpa {

    private int antelacionMinHoras;

    @Enumerated(EnumType.STRING)
    private BaseRetencion base;

    private Integer porcentaje;    // null si es monto fijo
    private BigDecimal montoFijo;  // null si es porcentual
}
