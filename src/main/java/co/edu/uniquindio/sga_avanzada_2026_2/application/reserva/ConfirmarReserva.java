package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: recepción confirma una reserva PENDIENTE (CU-18). El folio solo se lee para saber cuánto se ha
 * pagado (RES-15); solo se guarda la reserva.
 */
@Service
public class ConfirmarReserva {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;

    public ConfirmarReserva(CargadorReserva cargador, ReservaRepository reservas) {
        this.cargador = cargador;
        this.reservas = reservas;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su folio, su apartamento o su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si no está PENDIENTE
     *         (RN-08), falta la hora estimada de llegada (RN-09) o lo pagado no cubre el anticipo (RES-15)
     */
    @Transactional
    public ReservaResult ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        Alojamiento alojamiento = cargador.alojamientoDe(cargador.apartamento(reserva.apartamentoId()));

        reserva.confirmar(alojamiento.parametros().anticipo(), cargador.folioDe(reserva).totalPagado());

        reservas.guardar(reserva);
        return ReservaResult.de(reserva);
    }
}
