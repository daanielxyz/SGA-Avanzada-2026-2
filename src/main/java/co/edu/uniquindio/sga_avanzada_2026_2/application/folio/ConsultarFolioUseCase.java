package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CargadorReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: consultar el folio de una reserva con sus cargos, pagos y saldo.
 */
@Service
public class ConsultarFolioUseCase {

    private final CargadorReserva cargador;

    public ConsultarFolioUseCase(CargadorReserva cargador) {
        this.cargador = cargador;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva o su folio
     */
    @Transactional(readOnly = true)
    public FolioResult ejecutar(String codigoReserva) {
        return FolioResult.de(cargador.folioDe(cargador.reserva(codigoReserva)));
    }
}
