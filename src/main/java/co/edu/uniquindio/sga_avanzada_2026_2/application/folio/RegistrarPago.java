package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CargadorReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Caso de uso: recepción registra un abono en el folio de una reserva (RN-15). Sirve para el anticipo antes de la
 * llegada y para los pagos parciales durante la estancia (PAG-05).
 */
@Service
public class RegistrarPago {

    private final CargadorReserva cargador;
    private final FolioRepository folios;
    private final Clock reloj;

    public RegistrarPago(CargadorReserva cargador, FolioRepository folios, Clock reloj) {
        this.cargador = cargador;
        this.folios = folios;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su folio, su apartamento o su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el folio está cerrado
     *         (FOL-06), el monto no es positivo (PAG-02), la fecha es futura (PAG-07) o el medio no está habilitado
     *         en el alojamiento (MPAG-02)
     */
    @Transactional
    public FolioResult ejecutar(RegistrarPagoCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigoReserva());
        Folio folio = cargador.folioDe(reserva);
        Alojamiento alojamiento = cargador.alojamientoDe(reserva);

        folio.registrarPago(new MedioPago(comando.medio()), new Dinero(comando.monto()), comando.fecha(),
                LocalDate.now(reloj), alojamiento.mediosPago());

        folios.guardar(folio);
        return FolioResult.de(folio);
    }
}
