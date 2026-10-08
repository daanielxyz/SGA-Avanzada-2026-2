package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.AutorizacionCierre;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.SalidaId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Servicio de dominio sin estado: registra la salida (check-out) con la barrera financiera de RN-17. Cruza Reserva,
 * Apartamento y Folio, que recibe por parámetro y cambian juntos en la misma transacción (SAL-03 · D-02).
 */
public class SalidaOperativaDomainService {

    /**
     * Registra la salida exigiendo el folio cerrado (SAL-02): si sigue abierto lo cierra, lo que solo es posible con
     * saldo cero (RN-17 · FOL-05); si ya se cerró antes (CU-28) lo acepta (DEC-46). Luego aplica los efectos
     * inseparables: reserva FINALIZADA con sus noches restantes liberadas y apartamento PENDIENTE_PREPARACION (SAL-03 ·
     * SAL-06). Una salida anticipada no es cancelación (SAL-04).
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas; quedan como hora real de la salida (SAL-05)
     * @param autor recepcionista que registra (SAL-05)
     * @throws ReglaDominioException si el apartamento o el folio no son de la reserva, el folio abierto tiene saldo
     *                               (RN-17), la reserva no está EN_CURSO (SAL-01) o la hora es anterior al registro
     *                               (SAL-05)
     */
    public void procesarCheckOut(Reserva reserva, Apartamento apartamento, Folio folio, SalidaId salidaId,
                                 LocalDateTime ahora, UsuarioId autor) {
        validarCorrespondencia(reserva, apartamento, folio);
        reserva.validarSalida(ahora);
        if (!folio.cerrado()) {
            folio.cerrar();
        }
        finalizar(reserva, apartamento, salidaId, ahora, autor);
    }

    /**
     * Igual que {@link #procesarCheckOut(Reserva, Apartamento, Folio, SalidaId, LocalDateTime, UsuarioId)}, pero
     * cierra el folio con saldo distinto de cero gracias a la autorización del administrador (RN-17 · AUTC-01 ·
     * CU-38). La autorización no cambia el saldo (AUTC-04).
     *
     * @throws ReglaDominioException si el apartamento o el folio no son de la reserva, el folio ya estaba cerrado, la
     *                               reserva no está EN_CURSO (SAL-01) o la hora es anterior al registro (SAL-05)
     */
    public void procesarCheckOut(Reserva reserva, Apartamento apartamento, Folio folio, SalidaId salidaId,
                                 LocalDateTime ahora, UsuarioId autor, AutorizacionCierre autorizacion) {
        Objects.requireNonNull(autorizacion, "autorizacion");
        validarCorrespondencia(reserva, apartamento, folio);
        reserva.validarSalida(ahora);
        folio.cerrar(autorizacion);
        finalizar(reserva, apartamento, salidaId, ahora, autor);
    }

    // SAL-03 · SAL-06: la reserva ya se validó antes de cerrar el folio, así un rechazo no deja nada a medias
    private static void finalizar(Reserva reserva, Apartamento apartamento, SalidaId salidaId, LocalDateTime ahora,
                                  UsuarioId autor) {
        reserva.registrarSalida(salidaId, ahora, autor);
        apartamento.cambiarEstadoOperativo(EstadoOperativo.PENDIENTE_PREPARACION);
    }

    // FOL-02 · D-02: los tres agregados deben ser de la misma estancia
    private static void validarCorrespondencia(Reserva reserva, Apartamento apartamento, Folio folio) {
        RegistroLlegadaDomainService.validarApartamento(reserva, apartamento);
        if (!folio.reservaId().equals(reserva.codigo())) {
            throw new ReglaDominioException("El folio " + folio.id().valor() + " no es de la reserva "
                    + reserva.codigo().valor());
        }
    }
}
