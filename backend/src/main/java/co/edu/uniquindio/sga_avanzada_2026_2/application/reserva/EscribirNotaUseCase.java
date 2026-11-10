package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
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
 * Caso de uso: dejarle una nota al siguiente turno en una reserva activa (BL-08 · DEC-105). Devuelve todas las notas
 * de la reserva, para que la pantalla no tenga que volver a pedirlas.
 */
@Service
public class EscribirNotaUseCase {

    private final CargadorReserva cargador;
    private final NotaReservaRepository notas;
    private final GeneradorCodigos codigos;
    private final DirectorioPersonal personal;
    private final Clock reloj;

    public EscribirNotaUseCase(CargadorReserva cargador, NotaReservaRepository notas, GeneradorCodigos codigos,
                               DirectorioPersonal personal, Clock reloj) {
        this.cargador = cargador;
        this.notas = notas;
        this.codigos = codigos;
        this.personal = personal;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no existe
     *                                                                                                  la reserva
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva no está
     *                                                                                       activa
     */
    @Transactional
    public List<NotaResult> ejecutar(EscribirNotaCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigoReserva());

        NotaReserva nota = NotaReserva.escribir(new NotaReservaId(codigos.siguiente(SerieCodigo.NOTA)), reserva,
                comando.texto(), new UsuarioId(comando.autor()), LocalDateTime.now(reloj));

        notas.guardar(nota);
        return NotaResult.de(notas.listarPorReserva(reserva.codigo()), personal);
    }
}
