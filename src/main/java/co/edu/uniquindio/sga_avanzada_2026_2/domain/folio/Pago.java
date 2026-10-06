package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.LocalDate;

/**
 * Entidad interna de Folio, inmutable: monto siempre positivo; el tipo da el efecto (PAG-02 · PAG-08). Un ABONO baja
 * el saldo; un REVERSO lo sube y corrige un pago (PAG-03) o registra una devolución (PAG-06).
 */
public class Pago {

    private final PagoId id;
    private final MedioPago medio;
    private final Dinero monto; // > 0
    private final TipoPago tipo;
    private final LocalDate fecha;
    private final PagoId reversaDe; // solo REVERSO: el pago que corrige; null en una devolución (TPAG-02)

    // PAG-01 · PAG-02 · TPAG-02
    public Pago(PagoId id, MedioPago medio, Dinero monto, TipoPago tipo, LocalDate fecha, PagoId reversaDe) {
        if (id == null || tipo == null) {
            throw new ReglaDominioException("El pago requiere id y tipo");
        }
        if (medio == null || fecha == null) {
            throw new ReglaDominioException("Todo pago indica medio de pago y fecha");
        }
        if (monto == null || !monto.esPositivo()) {
            throw new ReglaDominioException("El monto del pago debe ser mayor que cero");
        }
        if (tipo == TipoPago.ABONO && reversaDe != null) {
            throw new ReglaDominioException("Solo un REVERSO referencia otro pago");
        }
        this.id = id;
        this.medio = medio;
        this.monto = monto;
        this.tipo = tipo;
        this.fecha = fecha;
        this.reversaDe = reversaDe;
    }

    public boolean esReverso() {
        return tipo == TipoPago.REVERSO;
    }

    public PagoId id() {
        return id;
    }

    public MedioPago medio() {
        return medio;
    }

    public Dinero monto() {
        return monto;
    }

    public TipoPago tipo() {
        return tipo;
    }

    public LocalDate fecha() {
        return fecha;
    }

    public PagoId reversaDe() {
        return reversaDe;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pago otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
