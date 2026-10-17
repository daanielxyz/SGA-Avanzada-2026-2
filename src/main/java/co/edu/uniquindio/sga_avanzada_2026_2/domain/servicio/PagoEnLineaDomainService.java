package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.LocalDateTime;

/**
 * El pago en línea de una reserva PENDIENTE (DEC-69): cruza la reserva, su folio y el alojamiento. Todo se valida
 * antes de cobrar, para no cobrar algo que después no se pueda registrar.
 */
public class PagoEnLineaDomainService {

    /** El medio con el que entra al folio todo pago en línea; el alojamiento debe tenerlo habilitado (MPAG-02). */
    public static final MedioPago MEDIO = new MedioPago("PAGO_EN_LINEA");

    /**
     * Cuánto falta cobrar en línea: lo que exige el alojamiento (anticipo o total) menos lo ya pagado (DEC-69).
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si el folio es de otra reserva (FOL-02), la reserva no está PENDIENTE (RN-08) o
     *                               ya venció su plazo (RN-21), el alojamiento no acepta pagos en línea (MPAG-02),
     *                               el folio está cerrado (FOL-06) o ya no falta nada por pagar
     */
    public Dinero montoACobrar(Reserva reserva, Folio folio, Alojamiento alojamiento, LocalDateTime ahora) {
        ParametrosAlojamiento parametros = alojamiento.parametros();
        if (!folio.reservaId().equals(reserva.codigo())) {
            throw new ReglaDominioException("El folio " + folio.id().valor() + " no es de la reserva "
                    + reserva.codigo().valor());
        }
        if (reserva.estado() != EstadoReserva.PENDIENTE) {
            throw new ReglaDominioException("Solo se paga en línea una reserva PENDIENTE; está " + reserva.estado());
        }
        if (reserva.vencio(ahora, parametros.plazoParaConfirmar(reserva.canalOrigen()))) {
            throw new ReglaDominioException("La reserva " + reserva.codigo().valor()
                    + " ya venció: el plazo para pagarla terminó");
        }
        exigirHabilitado(alojamiento);
        if (folio.cerrado()) {
            throw new ReglaDominioException("El folio de la reserva está cerrado");
        }
        Dinero exigido = parametros.pagoEnLinea().exigido(reserva.valorTotal(), parametros.anticipo());
        if (!folio.totalPagado().esMenorQue(exigido)) {
            throw new ReglaDominioException("La reserva ya tiene pagado lo que se exige; confírmela en recepción");
        }
        return exigido.restar(folio.totalPagado());
    }

    /**
     * MPAG-02 — sin el medio {@code PAGO_EN_LINEA} habilitado no se puede reservar por el portal, porque la reserva
     * no se podría pagar.
     *
     * @throws ReglaDominioException si el alojamiento no lo tiene habilitado
     */
    public void exigirHabilitado(Alojamiento alojamiento) {
        if (!alojamiento.aceptaMedioPago(MEDIO)) {
            throw new ReglaDominioException("El alojamiento no tiene habilitado el pago en línea");
        }
    }

    /**
     * Registra en el folio el pago aprobado por la pasarela y confirma la reserva (RN-08 · RES-15 · DEC-69). Toca la
     * reserva y su folio en la misma transacción, como la creación y la cancelación (DEC-44).
     *
     * @param monto el de {@link #montoACobrar}, ya cobrado
     * @throws ReglaDominioException si falta la hora estimada de llegada (RN-09) o el pago no cubre el anticipo
     */
    public void registrar(Reserva reserva, Folio folio, Alojamiento alojamiento, Dinero monto, LocalDateTime ahora) {
        folio.registrarPago(MEDIO, monto, ahora.toLocalDate(), ahora.toLocalDate(), alojamiento.mediosPago());
        reserva.confirmar(alojamiento.parametros().anticipo(), folio.totalPagado());
    }
}
