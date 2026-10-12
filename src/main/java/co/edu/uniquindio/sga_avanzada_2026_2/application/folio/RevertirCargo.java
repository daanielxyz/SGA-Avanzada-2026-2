package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CargadorReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.CargoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Caso de uso: corregir un cargo mal registrado con un ajuste en sentido contrario; el cargo original se conserva
 * (RN-16 · CAR-03).
 */
@Service
public class RevertirCargo {

    private final CargadorReserva cargador;
    private final FolioRepository folios;
    private final Clock reloj;

    public RevertirCargo(CargadorReserva cargador, FolioRepository folios, Clock reloj) {
        this.cargador = cargador;
        this.folios = folios;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva o su folio
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el folio está cerrado
     *         (CAR-07), no tiene ese cargo, ya fue corregido o es él mismo una corrección
     */
    @Transactional
    public FolioResult ejecutar(RevertirCargoCommand comando) {
        Folio folio = cargador.folioDe(cargador.reserva(comando.codigoReserva()));

        folio.revertirCargo(new CargoId(comando.cargoId()), comando.motivo(), LocalDate.now(reloj));

        folios.guardar(folio);
        return FolioResult.de(folio);
    }
}
