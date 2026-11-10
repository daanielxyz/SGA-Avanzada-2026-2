package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.NotaReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: las notas de una reserva para el siguiente turno, de la más reciente a la más antigua, pendientes y
 * atendidas (BL-08 · DEC-105).
 */
@Service
public class ListarNotasUseCase {

    private final CargadorReserva cargador;
    private final NotaReservaRepository notas;
    private final DirectorioPersonal personal;

    public ListarNotasUseCase(CargadorReserva cargador, NotaReservaRepository notas, DirectorioPersonal personal) {
        this.cargador = cargador;
        this.notas = notas;
        this.personal = personal;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no existe
     *                                                                                                  la reserva
     */
    @Transactional(readOnly = true)
    public List<NotaResult> ejecutar(String codigoReserva) {
        Reserva reserva = cargador.reserva(codigoReserva);
        return NotaResult.de(notas.listarPorReserva(reserva.codigo()), personal);
    }
}
