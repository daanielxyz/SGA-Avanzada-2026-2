package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.math.BigDecimal;

/**
 * Una reserva con el nombre de su titular, para que recepción la identifique sin abrir otra pantalla (CU-25 · CU-49).
 *
 * @param titularNombre  el nombre vigente del titular; la reserva solo guarda su id (referencia entre agregados)
 * @param anticipoExigido lo que debe estar pagado para confirmarla (RES-15); solo en el detalle, nulo en el listado
 */
public record ReservaConTitularResult(ReservaResult reserva, String titularNombre, BigDecimal anticipoExigido) {

    public ReservaConTitularResult(ReservaResult reserva, String titularNombre) {
        this(reserva, titularNombre, null);
    }
}
