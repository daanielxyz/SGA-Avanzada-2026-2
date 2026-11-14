package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Una reserva con el nombre de su titular, para que recepción la identifique sin abrir otra pantalla (CU-25 · CU-49).
 *
 * @param titularNombre  el nombre vigente del titular; la reserva solo guarda su id (referencia entre agregados)
 * @param anticipoExigido lo que debe estar pagado para confirmarla (RES-15); solo en el detalle, nulo en el listado
 * @param noShow         quién lo declaró y cuándo (BL-25); solo en el detalle; nulo si no aplica o no se guardó
 * @param correccionesDocumento los documentos corregidos (DEC-120); solo en el detalle para el personal
 */
public record ReservaConTitularResult(ReservaResult reserva, String titularNombre, BigDecimal anticipoExigido,
                                      DeclaracionNoShowResult noShow,
                                      List<CorreccionDocumentoResult> correccionesDocumento) {

    /** @param autorNombre el nombre vigente de esa persona; nulo si ya no existe */
    public record DeclaracionNoShowResult(String autor, String autorNombre, LocalDateTime fechaHora) {
    }

    /**
     * @param ocupante    nombre de la persona cuando se corrigió
     * @param autorNombre el nombre vigente de quien corrigió; nulo si ya no existe
     * @param titular     si fue el documento del titular
     */
    public record CorreccionDocumentoResult(String ocupante, String tipoAnterior, String numeroAnterior,
                                            String tipoNuevo, String numeroNuevo, String motivo, String autor,
                                            String autorNombre, LocalDateTime fechaHora, boolean titular) {
    }

    public ReservaConTitularResult(ReservaResult reserva, String titularNombre) {
        this(reserva, titularNombre, null, null, null);
    }

    /**
     * Lo que ve el huésped: quién del personal declaró el no-show o corrigió un documento no es dato suyo
     * (USU-04 · DEC-103 · DEC-120).
     */
    public ReservaConTitularResult sinDatosDelPersonal() {
        return new ReservaConTitularResult(reserva, titularNombre, anticipoExigido, null, null);
    }
}
