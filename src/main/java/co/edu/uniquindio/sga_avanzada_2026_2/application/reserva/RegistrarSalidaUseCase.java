package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.SalidaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.SalidaOperativaDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: recepción registra la salida del grupo (CU-22), o el administrador la autoriza con saldo pendiente
 * (CU-38). Cierra el folio, finaliza la reserva y deja el apartamento PENDIENTE_PREPARACION en la misma transacción
 * (SAL-03 · D-02).
 */
@Service
public class RegistrarSalidaUseCase {

    // DEC-40: una reserva tiene a lo sumo una salida
    private static final SalidaId SALIDA = new SalidaId("SAL-1");

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final ApartamentoRepository apartamentos;
    private final FolioRepository folios;
    private final SalidaOperativaDomainService salidaOperativa;
    private final Clock reloj;

    public RegistrarSalidaUseCase(CargadorReserva cargador, ReservaRepository reservas,
                                  ApartamentoRepository apartamentos, FolioRepository folios,
                                  SalidaOperativaDomainService salidaOperativa, Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.apartamentos = apartamentos;
        this.folios = folios;
        this.salidaOperativa = salidaOperativa;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su apartamento o su folio
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva no está
     *         EN_CURSO (SAL-01), el folio tiene saldo y no llegó el motivo de la autorización (RN-17), o llegó el
     *         motivo y el folio ya estaba cerrado
     */
    @Transactional
    public EstanciaResult ejecutar(RegistrarSalidaCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigo());
        Apartamento apartamento = cargador.apartamento(reserva.apartamentoId());
        Folio folio = cargador.folioDe(reserva);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        UsuarioId autor = new UsuarioId(comando.autor());

        comando.autorizacion(ahora).ifPresentOrElse(
                autorizacion -> salidaOperativa.procesarCheckOut(reserva, apartamento, folio, SALIDA, ahora, autor,
                        autorizacion),
                () -> salidaOperativa.procesarCheckOut(reserva, apartamento, folio, SALIDA, ahora, autor));

        reservas.guardar(reserva);
        apartamentos.guardar(apartamento);
        folios.guardar(folio);
        return EstanciaResult.de(reserva, apartamento, folio);
    }
}
