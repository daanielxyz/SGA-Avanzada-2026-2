package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * VO: valor en pesos colombianos (COP), sin decimales y con precisión exacta (DIN-01).
 * Puede ser negativo; cada consumidor decide si lo admite (DIN-03: solo ajustes y saldo a favor).
 */
public record Dinero(BigDecimal monto) {

    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    // DIN-01 · DIN-02: se redondea al peso más cercano al construir. Los cálculos con fracciones
    // (porcentajes) se hacen sobre el total del cargo y se construyen una sola vez, nunca noche por noche.
    public Dinero {
        if (monto == null) {
            throw new ReglaDominioException("El monto es obligatorio");
        }
        monto = monto.setScale(0, RoundingMode.HALF_UP);
    }

    public static Dinero de(long pesos) {
        return new Dinero(BigDecimal.valueOf(pesos));
    }

    /**
     * Suma dos valores (DIN-02).
     *
     * @return un Dinero nuevo; los operandos no cambian
     */
    public Dinero sumar(Dinero otro) {
        Objects.requireNonNull(otro, "otro");
        return new Dinero(monto.add(otro.monto));
    }

    /**
     * Resta dos valores (DIN-02). El resultado puede ser negativo (DIN-03).
     *
     * @return un Dinero nuevo; los operandos no cambian
     */
    public Dinero restar(Dinero otro) {
        Objects.requireNonNull(otro, "otro");
        return new Dinero(monto.subtract(otro.monto));
    }

    /**
     * Multiplica por una cantidad entera, p. ej. tarifa × ocupantes facturables (DIN-02 · RN-05).
     *
     * @return un Dinero nuevo; el operando no cambia
     */
    public Dinero multiplicar(int factor) {
        return new Dinero(monto.multiply(BigDecimal.valueOf(factor)));
    }

    public boolean esNegativo() {
        return monto.signum() < 0;
    }

    public boolean esPositivo() {
        return monto.signum() > 0;
    }

    public boolean esCero() {
        return monto.signum() == 0;
    }
}
