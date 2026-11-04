package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.AutorizacionCierre;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Cargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Pago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Salida de los casos de uso del Folio (DEC-31): movimientos, total pagado y saldo calculado (RN-15).
 *
 * @param situacionSaldo PENDIENTE (debe el huésped), A_FAVOR (hay que devolver) o AL_DIA
 * @param autorizacion   solo si se cerró con saldo distinto de cero (RN-17)
 */
public record FolioResult(String id, String reserva, List<CargoResult> cargos, List<PagoResult> pagos,
                          BigDecimal totalPagado, BigDecimal saldo, String situacionSaldo, boolean cerrado,
                          AutorizacionResult autorizacion) {

    /**
     * @param sentido  solo en un AJUSTE: AUMENTA o DISMINUYE
     * @param corrigeA el cargo que este AJUSTE revierte, si corrige alguno
     */
    public record CargoResult(String id, String tipo, String concepto, BigDecimal valor, String sentido,
                              LocalDate fecha, String corrigeA) {

        static CargoResult de(Cargo cargo) {
            return new CargoResult(cargo.id().valor(), cargo.tipo().name(), cargo.concepto(), cargo.valor().monto(),
                    Optional.ofNullable(cargo.sentido()).map(Enum::name).orElse(null), cargo.fecha(),
                    Optional.ofNullable(cargo.corrigeA()).map(c -> c.valor()).orElse(null));
        }
    }

    /**
     * @param tipo      ABONO o REVERSO
     * @param reversaDe el abono que este REVERSO corrige; vacío en una devolución
     */
    public record PagoResult(String id, String medio, BigDecimal monto, String tipo, LocalDate fecha,
                             String reversaDe) {

        static PagoResult de(Pago pago) {
            return new PagoResult(pago.id().valor(), pago.medio().nombre(), pago.monto().monto(), pago.tipo().name(),
                    pago.fecha(), Optional.ofNullable(pago.reversaDe()).map(p -> p.valor()).orElse(null));
        }
    }

    public record AutorizacionResult(String autor, String motivo, LocalDateTime fechaHora) {

        static AutorizacionResult de(AutorizacionCierre autorizacion) {
            return new AutorizacionResult(autorizacion.autor().valor(), autorizacion.motivo(),
                    autorizacion.fechaHora());
        }
    }

    public static FolioResult de(Folio folio) {
        Saldo saldo = folio.saldo();
        return new FolioResult(
                folio.id().valor(),
                folio.reservaId().valor(),
                folio.cargos().stream().map(CargoResult::de).toList(),
                folio.pagos().stream().map(PagoResult::de).toList(),
                folio.totalPagado().monto(),
                saldo.monto().monto(),
                saldo.situacion().name(),
                folio.cerrado(),
                Optional.ofNullable(folio.autorizacion()).map(AutorizacionResult::de).orElse(null));
    }
}
