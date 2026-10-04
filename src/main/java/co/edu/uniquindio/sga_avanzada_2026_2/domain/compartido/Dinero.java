package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * VO: valor en pesos colombianos (COP), sin decimales.
 */
public record Dinero(BigDecimal monto) {

    public Dinero {
        if (monto == null) {
            throw new ReglaDominioException("El monto es obligatorio");
        }
        monto = monto.setScale(0, RoundingMode.HALF_UP);
    }
}
