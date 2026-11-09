package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: recepción designa a otro ocupante como huésped principal del TRA, por ejemplo en la llegada (DEC-97).
 * Solo cambia la Reserva.
 */
@Service
public class DesignarHuespedPrincipalUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;

    public DesignarHuespedPrincipalUseCase(CargadorReserva cargador, ReservaRepository reservas) {
        this.cargador = cargador;
        this.reservas = reservas;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva ya terminó,
     *         el ocupante no es suyo, ya es el principal o es menor de edad (DEC-97)
     */
    @Transactional
    public ReservaResult ejecutar(String codigo, String ocupanteId) {
        Reserva reserva = cargador.reserva(codigo);

        reserva.designarHuespedPrincipal(new OcupanteId(ocupanteId));

        reservas.guardar(reserva);
        return ReservaResult.de(reserva);
    }
}
