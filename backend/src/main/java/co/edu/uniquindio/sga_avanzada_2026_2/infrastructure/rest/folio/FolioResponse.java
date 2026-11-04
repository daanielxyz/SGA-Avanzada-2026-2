package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.FolioResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JSON del folio de una reserva (DEC-31): movimientos, total pagado y saldo calculado (RN-15).
 *
 * @param situacionSaldo PENDIENTE (debe el huésped), A_FAVOR (hay que devolver) o AL_DIA
 * @param autorizacion   solo si se cerró con saldo distinto de cero (RN-17)
 */
public record FolioResponse(String id, String reserva, List<CargoResponse> cargos, List<PagoResponse> pagos,
                            BigDecimal totalPagado, BigDecimal saldo, String situacionSaldo, boolean cerrado,
                            AutorizacionResponse autorizacion) {

    /**
     * @param sentido  solo en un AJUSTE: AUMENTA o DISMINUYE
     * @param corrigeA el cargo que este AJUSTE revierte, si corrige alguno
     */
    public record CargoResponse(String id, String tipo, String concepto, BigDecimal valor, String sentido,
                                LocalDate fecha, String corrigeA) {
    }

    /**
     * @param tipo      ABONO o REVERSO
     * @param reversaDe el abono que este REVERSO corrige; vacío en una devolución
     */
    public record PagoResponse(String id, String medio, BigDecimal monto, String tipo, LocalDate fecha,
                               String reversaDe) {
    }

    public record AutorizacionResponse(String autor, String motivo, LocalDateTime fechaHora) {
    }

    static FolioResponse de(FolioResult r) {
        return new FolioResponse(r.id(), r.reserva(),
                r.cargos().stream()
                        .map(c -> new CargoResponse(c.id(), c.tipo(), c.concepto(), c.valor(), c.sentido(), c.fecha(),
                                c.corrigeA()))
                        .toList(),
                r.pagos().stream()
                        .map(p -> new PagoResponse(p.id(), p.medio(), p.monto(), p.tipo(), p.fecha(), p.reversaDe()))
                        .toList(),
                r.totalPagado(), r.saldo(), r.situacionSaldo(), r.cerrado(),
                Optional.ofNullable(r.autorizacion())
                        .map(a -> new AutorizacionResponse(a.autor(), a.motivo(), a.fechaHora()))
                        .orElse(null));
    }
}
