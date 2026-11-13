package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el personal lee la estancia de una reserva (#13 · DEC-114): los registros de llegada, el vigente y los
 * anulados por una corrección, con su autor, su nombre y el motivo de entrar sin el pago completo; la salida, si la
 * hubo, y el saldo del folio. Solo lee: no cambia nada. El huésped no lo ve (DEC-99).
 */
@Service
public class ConsultarEstanciaUseCase {

    private final CargadorReserva cargador;
    private final DirectorioPersonal personal;

    public ConsultarEstanciaUseCase(CargadorReserva cargador, DirectorioPersonal personal) {
        this.cargador = cargador;
        this.personal = personal;
    }

    /**
     * Una reserva que todavía no llegó (o que no llegará) responde con la lista de registros vacía y sin salida.
     *
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su apartamento o su folio
     */
    @Transactional(readOnly = true)
    public EstanciaResult ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        return EstanciaResult.de(reserva, cargador.apartamento(reserva.apartamentoId()), cargador.folioDe(reserva),
                personal);
    }
}
