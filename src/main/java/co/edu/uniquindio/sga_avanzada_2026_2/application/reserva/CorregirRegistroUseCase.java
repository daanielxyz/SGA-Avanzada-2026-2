package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: corregir un registro de llegada equivocado (REG-06 · DEC-45). El registro vigente queda anulado
 * como historial y se agrega el correcto; solo cambia la Reserva.
 */
@Service
public class CorregirRegistroUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final Clock reloj;

    public CorregirRegistroUseCase(CargadorReserva cargador, ReservaRepository reservas, Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su apartamento o su folio
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva no está
     *         EN_CURSO, falta el motivo, o la fecha corregida es anterior a la entrada (REG-02) o posterior a ahora
     */
    @Transactional
    public EstanciaResult ejecutar(CorregirRegistroCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigo());

        reserva.corregirRegistro(RegistrarLlegadaUseCase.siguienteRegistro(reserva), comando.fechaHora(),
                new UsuarioId(comando.autor()), comando.motivo(), LocalDateTime.now(reloj));

        reservas.guardar(reserva);
        return EstanciaResult.de(reserva, cargador.apartamento(reserva.apartamentoId()), cargador.folioDe(reserva));
    }
}
