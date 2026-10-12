package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Registro;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Salida;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Salida de la llegada, su corrección y la salida (DEC-31): cómo quedan juntos la reserva, el apartamento y el
 * folio (D-02).
 *
 * @param registros      historial de registros de llegada; el vigente es el único no anulado (REG-06)
 * @param salida         solo cuando la reserva está FINALIZADA
 * @param situacionSaldo PENDIENTE (debe el huésped), A_FAVOR (hay que devolver) o AL_DIA
 */
public record EstanciaResult(String codigo, String estado, String apartamento, String estadoOperativo,
                             List<RegistroResult> registros, SalidaResult salida, boolean folioCerrado,
                             BigDecimal saldo, String situacionSaldo) {

    /**
     * @param motivoCorreccion solo si este registro corrige a uno anulado
     */
    public record RegistroResult(String id, LocalDateTime fechaHora, String autor, boolean anulado,
                                 String motivoCorreccion) {

        static RegistroResult de(Registro registro) {
            return new RegistroResult(registro.id().valor(), registro.fechaHora(), registro.autor().valor(),
                    registro.anulado(), registro.motivoCorreccion());
        }
    }

    public record SalidaResult(String id, LocalDateTime fechaHora, String autor) {

        static SalidaResult de(Salida salida) {
            return new SalidaResult(salida.id().valor(), salida.fechaHora(), salida.autor().valor());
        }
    }

    static EstanciaResult de(Reserva reserva, Apartamento apartamento, Folio folio) {
        Saldo saldo = folio.saldo();
        return new EstanciaResult(
                reserva.codigo().valor(),
                reserva.estado().name(),
                apartamento.codigo().valor(),
                apartamento.estadoOperativo().name(),
                reserva.registros().stream().map(RegistroResult::de).toList(),
                Optional.ofNullable(reserva.salida()).map(SalidaResult::de).orElse(null),
                folio.cerrado(),
                saldo.monto().monto(),
                saldo.situacion().name());
    }
}
