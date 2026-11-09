package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: recepción cambia el titular cuando quien reservó no debe serlo (TIT-01 · DEC-97). Guarda la Reserva
 * y, como al crearla, el titular nuevo o con su contacto completado (DEC-58 · DEC-69).
 */
@Service
public class CambiarTitularUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final TitularRepository titulares;
    private final AvisosReserva avisos;

    public CambiarTitularUseCase(CargadorReserva cargador, ReservaRepository reservas, TitularRepository titulares,
                                 AvisosReserva avisos) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.titulares = titulares;
        this.avisos = avisos;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva o su apartamento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva ya terminó,
     *         ya es su titular o sus datos no son válidos (DEC-49)
     */
    @Transactional
    public ReservaResult ejecutar(CambiarTitularCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigo());
        TitularParaReserva paraReserva = cargador.titularPara(
                cargador.alojamientoDe(reserva).id(), comando.titular());

        reserva.cambiarTitular(paraReserva.titular());

        titulares.guardar(paraReserva.titular());
        reservas.guardar(reserva);
        return ReservaResult.de(reserva).conAvisoCuenta(avisos.avisoCuenta(paraReserva));
    }
}
