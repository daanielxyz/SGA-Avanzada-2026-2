package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PoliticaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.PagoEnLineaDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: retomar el pago en línea de una reserva PENDIENTE, por ejemplo si se cerró la pantalla de pago antes
 * de terminar (DEC-69 · DEC-91). Con la misma regla que cobra {@link PagarEnLineaUseCase}.
 */
@Service
public class ConsultarPagoPendienteUseCase {

    private final CargadorReserva cargador;
    private final PagoEnLineaDomainService pagoEnLinea;
    private final PoliticaCancelacionRepository politicas;
    private final Clock reloj;

    public ConsultarPagoPendienteUseCase(CargadorReserva cargador, PagoEnLineaDomainService pagoEnLinea,
                                         PoliticaCancelacionRepository politicas, Clock reloj) {
        this.cargador = cargador;
        this.pagoEnLinea = pagoEnLinea;
        this.politicas = politicas;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe la reserva, su folio, su política congelada, su apartamento o
     *         su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva no está
     *         PENDIENTE o ya venció, el alojamiento no acepta pagos en línea o no falta nada por pagar
     */
    @Transactional(readOnly = true)
    public PagoPendienteResult ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        Alojamiento alojamiento = cargador.alojamientoDe(reserva);
        return new PagoPendienteResult(
                pagoEnLinea.montoACobrar(reserva, cargador.folioDe(reserva), alojamiento, LocalDateTime.now(reloj))
                        .monto(),
                reserva.venceEn(alojamiento.parametros().plazoParaConfirmar(reserva.canalOrigen())),
                PoliticaResult.de(politicas.buscarPorId(reserva.politicaVersionId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("la política",
                                reserva.politicaVersionId().valor()))));
    }
}
