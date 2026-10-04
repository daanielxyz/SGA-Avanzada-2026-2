package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;

import java.time.LocalDate;

/**
 * Entidad interna de Folio. Inmutable: se corrige con un REVERSO (monto positivo).
 */
public class Pago {

    private final PagoId id;
    private final MedioPago medio;
    private final Dinero monto; // > 0
    private final TipoPago tipo;
    private final LocalDate fecha;
    private final PagoId reversaDe; // opcional: solo si es REVERSO

    public Pago(PagoId id, MedioPago medio, Dinero monto, TipoPago tipo, LocalDate fecha, PagoId reversaDe) {
        this.id = id;
        this.medio = medio;
        this.monto = monto;
        this.tipo = tipo;
        this.fecha = fecha;
        this.reversaDe = reversaDe;
    }
}
