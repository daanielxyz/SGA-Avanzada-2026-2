package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: cancelar una reserva que no ha iniciado (CU-14 · CU-15). Retiene según la política congelada y
 * liquida en el folio, en la misma transacción (DEC-44).
 */
@Service
public class CancelarReserva {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final PoliticaCancelacionRepository politicas;
    private final CancelacionDomainService cancelacion;
    private final Clock reloj;

    public CancelarReserva(CargadorReserva cargador, ReservaRepository reservas, FolioRepository folios,
                           PoliticaCancelacionRepository politicas, CancelacionDomainService cancelacion,
                           Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.folios = folios;
        this.politicas = politicas;
        this.cancelacion = cancelacion;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe la reserva, su folio, su política congelada, su apartamento o
     *                                      su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva no está
     *                                      PENDIENTE ni CONFIRMADA (RN-08) o el folio está cerrado
     */
    @Transactional
    public CancelacionResult ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        Folio folio = cargador.folioDe(reserva);
        PoliticaCancelacion politica = politicas.buscarPorId(reserva.politicaVersionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("la política",
                        reserva.politicaVersionId().valor()));
        Alojamiento alojamiento = cargador.alojamientoDe(cargador.apartamento(reserva.apartamentoId()));

        Dinero retenido = cancelacion.procesarCancelacion(reserva, folio, politica, alojamiento.parametros(),
                LocalDateTime.now(reloj));

        reservas.guardar(reserva);
        folios.guardar(folio);
        return CancelacionResult.de(reserva, retenido, folio);
    }
}
