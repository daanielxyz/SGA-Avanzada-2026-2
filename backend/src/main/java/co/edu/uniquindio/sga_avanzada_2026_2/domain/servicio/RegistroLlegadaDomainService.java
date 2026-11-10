package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MotivoRechazo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.RegistroId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.LocalDateTime;

/**
 * Servicio de dominio sin estado: registra la llegada (check-in). Cruza Reserva y Apartamento, que recibe por
 * parámetro y cambian juntos en la misma transacción (REG-04 · D-02).
 */
public class RegistroLlegadaDomainService {

    /**
     * Garantiza la triple condición del registro: apartamento PREPARADO (RN-11 · EOPE-03 · REG-03), reserva
     * CONFIRMADA (RN-10 · REG-01) y fecha de entrada alcanzada (RN-10 · REG-02; llegar después es válido hasta el
     * no-show). Luego aplica los dos efectos inseparables: reserva EN_CURSO y apartamento OCUPADO (REG-04). Valida
     * el apartamento antes de tocar la reserva, así un rechazo no deja nada a medias.
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas; quedan como hora real del registro (REG-05)
     * @param autor recepcionista que registra (REG-05)
     * @param pagado        lo pagado en el folio hasta ahora (BL-22)
     * @param motivoSinPago por qué entra sin haber pagado; nulo si no aplica (BL-22)
     * @throws ReglaDominioException si el apartamento no es el de la reserva, no está PREPARADO (RN-11), la reserva
     *                               no está CONFIRMADA, aún no es la fecha de entrada (RN-10) o la estancia no está
     *                               pagada y no hay motivo (BL-22)
     */
    public void procesarCheckIn(Reserva reserva, Apartamento apartamento, RegistroId registroId, LocalDateTime ahora,
                                UsuarioId autor, Dinero pagado, String motivoSinPago) {
        validarApartamento(reserva, apartamento);
        if (!apartamento.estadoOperativo().permiteRegistro()) {
            throw new ReglaDominioException(MotivoRechazo.APARTAMENTO_NO_PREPARADO,
                    "El apartamento " + apartamento.codigo().valor() + " está "
                    + apartamento.estadoOperativo() + ": solo se entrega PREPARADO");
        }
        reserva.registrarLlegada(registroId, ahora, autor, pagado, motivoSinPago);
        apartamento.ocupar();
    }

    static void validarApartamento(Reserva reserva, Apartamento apartamento) {
        if (!reserva.apartamentoId().equals(apartamento.codigo())) {
            throw new ReglaDominioException("La reserva " + reserva.codigo().valor() + " es del apartamento "
                    + reserva.apartamentoId().valor() + ", no del " + apartamento.codigo().valor());
        }
    }
}
