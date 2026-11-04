package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * VO: valor en pesos colombianos (COP), sin decimales y con precisión exacta (DIN-01).
 * Nunca es negativo (DIN-03 · DEC-20).
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
        // DIN-03 · DEC-20
        if (monto.signum() < 0) {
            throw new ReglaDominioException("El dinero no puede ser negativo: " + monto);
        }
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
     * Resta dos valores (DIN-02).
     *
     * @return un Dinero nuevo; los operandos no cambian
     * @throws ReglaDominioException si {@code otro} es mayor y el resultado sería negativo (DIN-03)
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

    /**
     * Fracción porcentual del valor, p. ej. el anticipo sobre el total de la reserva (RES-15). Se redondea una sola
     * vez, al construir el resultado (DIN-02).
     *
     * @return un Dinero nuevo; el operando no cambia
     */
    public Dinero porcentaje(Porcentaje porcentaje) {
        Objects.requireNonNull(porcentaje, "porcentaje");
        return new Dinero(monto.multiply(BigDecimal.valueOf(porcentaje.valor())).divide(BigDecimal.valueOf(100)));
    }

    public boolean esMenorQue(Dinero otro) {
        Objects.requireNonNull(otro, "otro");
        return monto.compareTo(otro.monto) < 0;
    }

    public boolean esPositivo() {
        return monto.signum() > 0;
    }

    public boolean esCero() {
        return monto.signum() == 0;
    }
}
