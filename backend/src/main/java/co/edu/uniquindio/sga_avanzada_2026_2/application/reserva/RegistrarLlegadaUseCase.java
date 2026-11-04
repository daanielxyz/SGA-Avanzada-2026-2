package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.RegistroId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.RegistroLlegadaDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: recepción registra la llegada del grupo (CU-21). La reserva pasa a EN_CURSO y el apartamento a
 * OCUPADO en la misma transacción (REG-04 · D-02); el folio solo se lee para informar el saldo.
 */
@Service
public class RegistrarLlegadaUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final ApartamentoRepository apartamentos;
    private final RegistroLlegadaDomainService registroLlegada;
    private final Clock reloj;

    public RegistrarLlegadaUseCase(CargadorReserva cargador, ReservaRepository reservas,
                                   ApartamentoRepository apartamentos, RegistroLlegadaDomainService registroLlegada,
                                   Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.apartamentos = apartamentos;
        this.registroLlegada = registroLlegada;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su apartamento o su folio
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el apartamento no
     *         está PREPARADO (RN-11), la reserva no está CONFIRMADA o aún no es la fecha de entrada (RN-10)
     */
    @Transactional
    public EstanciaResult ejecutar(RegistrarLlegadaCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigo());
        Apartamento apartamento = cargador.apartamento(reserva.apartamentoId());

        registroLlegada.procesarCheckIn(reserva, apartamento, siguienteRegistro(reserva), LocalDateTime.now(reloj),
                new UsuarioId(comando.autor()));

        reservas.guardar(reserva);
        apartamentos.guardar(apartamento);
        return EstanciaResult.de(reserva, apartamento, cargador.folioDe(reserva));
    }

    /** Los registros solo deben ser únicos dentro de su reserva y nunca se borran: se numeran en orden (DEC-61). */
    static RegistroId siguienteRegistro(Reserva reserva) {
        return new RegistroId("REG-" + (reserva.registros().size() + 1));
    }
}
