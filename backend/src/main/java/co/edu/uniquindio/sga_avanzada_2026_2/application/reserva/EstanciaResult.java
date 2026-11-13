package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
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
 * Salida de la llegada, su corrección, la salida y la consulta de la estancia (DEC-31): cómo quedan juntos la
 * reserva, el apartamento y el folio (D-02). Solo para el personal: el motivo sin pago es interno (DEC-99 · DEC-114).
 *
 * @param registros      historial de registros de llegada; el vigente es el único no anulado (REG-06)
 * @param salida         solo cuando la reserva está FINALIZADA
 * @param situacionSaldo PENDIENTE (debe el huésped), A_FAVOR (hay que devolver) o AL_DIA
 */
public record EstanciaResult(String codigo, String estado, String apartamento, String estadoOperativo,
                             List<RegistroResult> registros, SalidaResult salida, boolean folioCerrado,
                             BigDecimal saldo, String situacionSaldo) {

    /**
     * @param autorNombre      el nombre vigente de quien registró; vacío si ya no existe (DEC-103 · DEC-114)
     * @param motivoCorreccion solo si este registro corrige a uno anulado
     * @param motivoSinPago    solo si entró sin haber pagado la estancia y sus extras (BL-22)
     */
    public record RegistroResult(String id, LocalDateTime fechaHora, String autor, String autorNombre,
                                 boolean anulado, String motivoCorreccion, String motivoSinPago) {

        static RegistroResult de(Registro registro, DirectorioPersonal personal) {
            return new RegistroResult(registro.id().valor(), registro.fechaHora(), registro.autor().valor(),
                    personal.nombreDe(registro.autor()).orElse(null), registro.anulado(),
                    registro.motivoCorreccion(), registro.motivoSinPago());
        }
    }

    /**
     * @param autorNombre el nombre vigente de quien registró la salida; vacío si ya no existe (DEC-114)
     */
    public record SalidaResult(String id, LocalDateTime fechaHora, String autor, String autorNombre) {

        static SalidaResult de(Salida salida, DirectorioPersonal personal) {
            return new SalidaResult(salida.id().valor(), salida.fechaHora(), salida.autor().valor(),
                    personal.nombreDe(salida.autor()).orElse(null));
        }
    }

    static EstanciaResult de(Reserva reserva, Apartamento apartamento, Folio folio, DirectorioPersonal personal) {
        Saldo saldo = folio.saldo();
        return new EstanciaResult(
                reserva.codigo().valor(),
                reserva.estado().name(),
                apartamento.codigo().valor(),
                apartamento.estadoOperativo().name(),
                reserva.registros().stream().map(r -> RegistroResult.de(r, personal)).toList(),
                Optional.ofNullable(reserva.salida()).map(s -> SalidaResult.de(s, personal)).orElse(null),
                folio.cerrado(),
                saldo.monto().monto(),
                saldo.situacion().name());
    }
}
