package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.PagoRechazadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.PasarelaPago;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.PasarelaPago.CobroEnLinea;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.PagoEnLineaDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Caso de uso: pagar en línea una reserva PENDIENTE (DEC-69). Valida todo antes de cobrar; si la pasarela aprueba,
 * registra el pago en el folio, confirma la reserva y avisa al titular.
 */
@Service
public class PagarEnLineaUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final PagoEnLineaDomainService pagoEnLinea;
    private final PasarelaPago pasarela;
    private final AvisosReserva avisos;
    private final Clock reloj;

    public PagarEnLineaUseCase(CargadorReserva cargador, ReservaRepository reservas, FolioRepository folios,
                               PagoEnLineaDomainService pagoEnLinea, PasarelaPago pasarela, AvisosReserva avisos,
                               Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.folios = folios;
        this.pagoEnLinea = pagoEnLinea;
        this.pasarela = pasarela;
        this.avisos = avisos;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su folio, su apartamento o su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva no está
     *         PENDIENTE o ya venció, el alojamiento no acepta pagos en línea o no falta nada por pagar; no se cobra
     * @throws PagoRechazadoException si la pasarela no aprueba el cobro; no se registra nada
     */
    @Transactional
    public ReservaResult ejecutar(PagarEnLineaCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigoReserva());
        Folio folio = cargador.folioDe(reserva);
        Alojamiento alojamiento = cargador.alojamientoDe(reserva);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Dinero monto = pagoEnLinea.montoACobrar(reserva, folio, alojamiento, ahora);

        CobroEnLinea cobro = pasarela.cobrar(reserva.codigo().valor(), monto, comando.tokenPago());
        Optional.of(cobro).filter(CobroEnLinea::aprobado)
                .orElseThrow(() -> new PagoRechazadoException(cobro.motivo()));
        pagoEnLinea.registrar(reserva, folio, alojamiento, monto, ahora);

        reservas.guardar(reserva);
        folios.guardar(folio);
        avisos.avisarPagoEnLinea(reserva, cargador.titular(reserva.titularId()), monto, alojamiento.nombre());
        return ReservaResult.de(reserva);
    }
}
