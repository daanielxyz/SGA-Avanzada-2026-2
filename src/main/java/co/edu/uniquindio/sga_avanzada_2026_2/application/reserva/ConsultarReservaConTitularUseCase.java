package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: ver el detalle de una reserva con el nombre de su titular (CU-49).
 */
@Service
public class ConsultarReservaConTitularUseCase {

    private final CargadorReserva cargador;
    private final TitularRepository titulares;

    public ConsultarReservaConTitularUseCase(CargadorReserva cargador, TitularRepository titulares) {
        this.cargador = cargador;
        this.titulares = titulares;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public ReservaConTitularResult ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        return new ReservaConTitularResult(ReservaResult.de(reserva),
                titulares.buscarPorId(reserva.titularId()).map(Titular::nombre).orElse(null));
    }
}
