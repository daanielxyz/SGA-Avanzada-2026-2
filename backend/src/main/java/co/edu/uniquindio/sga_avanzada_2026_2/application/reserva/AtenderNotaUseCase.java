package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.NotaReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.NotaReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.NotaReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: marcar como atendida una nota de la reserva, con quién y cuándo (BL-08 · DEC-105). Devuelve todas las
 * notas de la reserva.
 */
@Service
public class AtenderNotaUseCase {

    private final CargadorReserva cargador;
    private final NotaReservaRepository notas;
    private final DirectorioPersonal personal;
    private final Clock reloj;

    public AtenderNotaUseCase(CargadorReserva cargador, NotaReservaRepository notas, DirectorioPersonal personal,
                              Clock reloj) {
        this.cargador = cargador;
        this.notas = notas;
        this.personal = personal;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe la reserva, o la nota no existe o es de otra reserva
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si ya estaba atendida o
     *                                                                                       la reserva no está activa
     */
    @Transactional
    public List<NotaResult> ejecutar(AtenderNotaCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigoReserva());
        NotaReservaId id = new NotaReservaId(comando.nota());
        NotaReserva nota = notas.buscarPorId(id)
                .filter(n -> n.reservaId().equals(reserva.codigo()))
                .orElseThrow(() -> new RecursoNoEncontradoException("la nota", id.valor()));

        nota.atender(reserva, new UsuarioId(comando.autor()), LocalDateTime.now(reloj));

        notas.guardar(nota);
        return NotaResult.de(notas.listarPorReserva(reserva.codigo()), personal);
    }
}
