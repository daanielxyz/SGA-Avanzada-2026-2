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
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.NoShowDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: recepción declara que el titular no se presentó (CU-19). Aplica la penalización de no-show de la
 * política congelada y la liquida en el folio, en la misma transacción (DEC-44).
 */
@Service
public class DeclararNoShow {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final PoliticaCancelacionRepository politicas;
    private final NoShowDomainService noShow;
    private final Clock reloj;

    public DeclararNoShow(CargadorReserva cargador, ReservaRepository reservas, FolioRepository folios,
                          PoliticaCancelacionRepository politicas, NoShowDomainService noShow, Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.folios = folios;
        this.politicas = politicas;
        this.noShow = noShow;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe la reserva, su folio, su política congelada, su apartamento o
     *                                      su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva no está
     *                                      CONFIRMADA (RN-08) o aún no llega la hora límite (RES-16)
     */
    @Transactional
    public CancelacionResult ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        Folio folio = cargador.folioDe(reserva);
        PoliticaCancelacion politica = politicas.buscarPorId(reserva.politicaVersionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("la política",
                        reserva.politicaVersionId().valor()));
        Alojamiento alojamiento = cargador.alojamientoDe(cargador.apartamento(reserva.apartamentoId()));

        Dinero retenido = noShow.declararNoShow(reserva, folio, politica, alojamiento.parametros(),
                LocalDateTime.now(reloj));

        reservas.guardar(reserva);
        folios.guardar(folio);
        return CancelacionResult.de(reserva, retenido, folio);
    }
}
