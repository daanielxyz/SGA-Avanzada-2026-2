package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.PagoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TipoPago;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad (Hija de Folio): Representa un abono o reverso financiero registrado en el Folio.
 */
public class Pago {

    private final PagoId id;
    private final MedioPago medio;
    private final Dinero monto;
    private final TipoPago tipo;
    private final LocalDate fecha;

    private Pago(PagoId id, MedioPago medio, Dinero monto, TipoPago tipo, LocalDate fecha) {
        this.id = id;
        this.medio = medio;
        this.monto = monto;
        this.tipo = tipo;
        this.fecha = fecha;
    }

    public static Pago crear(PagoId id, MedioPago medio, Dinero monto, TipoPago tipo, LocalDate fecha) {
        Objects.requireNonNull(id, "El id del pago no puede ser nulo");
        Objects.requireNonNull(medio, "El medio de pago no puede ser nulo");
        Objects.requireNonNull(monto, "El monto de pago no puede ser nulo");
        if (!monto.esMayorQueCero()) {
            throw new ReglaDominioException("El monto del pago debe ser estrictamente mayor que cero");
        }
        Objects.requireNonNull(tipo, "El tipo de pago no puede ser nulo");
        Objects.requireNonNull(fecha, "La fecha de pago no puede ser nula");

        return new Pago(id, medio, monto, tipo, fecha);
    }

    public boolean esReverso() {
        return this.tipo == TipoPago.REVERSO;
    }

    public PagoId getId() {
        return id;
    }

    public MedioPago getMedio() {
        return medio;
    }

    public Dinero getMonto() {
        return monto;
    }

    public TipoPago getTipo() {
        return tipo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pago pago = (Pago) o;
        return Objects.equals(id, pago.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
