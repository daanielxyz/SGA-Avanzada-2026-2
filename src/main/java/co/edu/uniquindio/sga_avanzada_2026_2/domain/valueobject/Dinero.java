package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Objeto de valor: Representación monetaria en Pesos Colombianos (COP).
 * 
 * Justificación de diseño:
 * La moneda COP no maneja centavos, por lo que se normaliza siempre a escala 0 (RoundingMode.HALF_UP).
 * Para la creación de importes estándar (precios, pagos) el valor debe ser no negativo.
 * Sin embargo, se admite que el saldo neto resultante de operaciones contables o de ajustes
 * pueda ser negativo (representando saldos a favor del huésped o crédito en folio).
 */
public record Dinero(BigDecimal monto) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    public Dinero {
        Objects.requireNonNull(monto, "El monto de dinero no puede ser nulo");
        monto = monto.setScale(0, RoundingMode.HALF_UP);
    }

    public static Dinero de(long valor) {
        return new Dinero(BigDecimal.valueOf(valor));
    }

    public static Dinero de(double valor) {
        return new Dinero(BigDecimal.valueOf(valor));
    }

    public Dinero sumar(Dinero otro) {
        Objects.requireNonNull(otro, "El importe a sumar no puede ser nulo");
        return new Dinero(this.monto.add(otro.monto));
    }

    public Dinero restar(Dinero otro) {
        Objects.requireNonNull(otro, "El importe a restar no puede ser nulo");
        return new Dinero(this.monto.subtract(otro.monto));
    }

    public Dinero multiplicar(int factor) {
        return new Dinero(this.monto.multiply(BigDecimal.valueOf(factor)));
    }

    public boolean esCero() {
        return this.monto.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean esMayorQueCero() {
        return this.monto.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean esNegativo() {
        return this.monto.compareTo(BigDecimal.ZERO) < 0;
    }

    @Override
    public int compareTo(Dinero otro) {
        return this.monto.compareTo(otro.monto);
    }
}
