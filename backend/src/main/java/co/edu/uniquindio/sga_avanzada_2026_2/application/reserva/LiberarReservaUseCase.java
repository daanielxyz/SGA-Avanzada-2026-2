package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: quien reservó en el portal vuelve desde el pago para cambiar algo, y la reserva PENDIENTE se libera
 * de inmediato, para que sus noches no queden apartadas hasta que venza (DEC-93). Toca Reserva y Folio (DEC-44).
 */
@Service
public class LiberarReservaUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final CancelacionDomainService cancelacion;
    private final Clock reloj;

    public LiberarReservaUseCase(CargadorReserva cargador, ReservaRepository reservas, FolioRepository folios,
                                 CancelacionDomainService cancelacion, Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.folios = folios;
        this.cancelacion = cancelacion;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe la reserva o su folio
     * @throws ReglaDominioException        si la reserva no está PENDIENTE o ya tiene pagos
     */
    @Transactional
    public void ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        Folio folio = cargador.folioDe(reserva);

        cancelacion.procesarLiberacion(reserva, folio, LocalDateTime.now(reloj));

        reservas.guardar(reserva);
        folios.guardar(folio);
    }
}
