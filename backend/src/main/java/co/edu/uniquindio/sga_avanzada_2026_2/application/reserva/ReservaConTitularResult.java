package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una reserva con el nombre de su titular, para que recepción la identifique sin abrir otra pantalla (CU-25 · CU-49).
 *
 * @param titularNombre  el nombre vigente del titular; la reserva solo guarda su id (referencia entre agregados)
 * @param anticipoExigido lo que debe estar pagado para confirmarla (RES-15); solo en el detalle, nulo en el listado
 * @param noShow         quién lo declaró y cuándo (BL-25); solo en el detalle; nulo si no aplica o no se guardó
 */
public record ReservaConTitularResult(ReservaResult reserva, String titularNombre, BigDecimal anticipoExigido,
                                      DeclaracionNoShowResult noShow) {

    /** @param autorNombre el nombre vigente de esa persona; nulo si ya no existe */
    public record DeclaracionNoShowResult(String autor, String autorNombre, LocalDateTime fechaHora) {
    }

    public ReservaConTitularResult(ReservaResult reserva, String titularNombre) {
        this(reserva, titularNombre, null, null);
    }

    /** Lo que ve el huésped: quién del personal declaró el no-show no es dato suyo (USU-04 · DEC-103). */
    public ReservaConTitularResult sinDatosDelPersonal() {
        return new ReservaConTitularResult(reserva, titularNombre, anticipoExigido, null);
    }
}
