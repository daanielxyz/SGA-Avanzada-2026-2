package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CargadorReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.PagoId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Caso de uso: corregir un abono mal registrado con un reverso fechado hoy; el abono original se conserva (RN-16 ·
 * PAG-03).
 */
@Service
public class RevertirPago {

    private final CargadorReserva cargador;
    private final FolioRepository folios;
    private final Clock reloj;

    public RevertirPago(CargadorReserva cargador, FolioRepository folios, Clock reloj) {
        this.cargador = cargador;
        this.folios = folios;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva o su folio
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el folio está cerrado
     *         (FOL-06), no tiene ese pago, no es un abono, ya fue revertido o el monto supera lo abonado neto
     */
    @Transactional
    public FolioResult ejecutar(RevertirPagoCommand comando) {
        Folio folio = cargador.folioDe(cargador.reserva(comando.codigoReserva()));
        LocalDate hoy = LocalDate.now(reloj);

        folio.revertirPago(new PagoId(comando.pagoId()), hoy, hoy);

        folios.guardar(folio);
        return FolioResult.de(folio);
    }
}
