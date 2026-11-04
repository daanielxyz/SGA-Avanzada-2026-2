package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CargadorReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: cerrar el folio de una reserva sin esperar a la salida (CU-28), por ejemplo tras una cancelación ya
 * liquidada. Con saldo distinto de cero exige el motivo de la autorización (RN-17).
 */
@Service
public class CerrarFolioUseCase {

    private final CargadorReserva cargador;
    private final FolioRepository folios;
    private final Clock reloj;

    public CerrarFolioUseCase(CargadorReserva cargador, FolioRepository folios, Clock reloj) {
        this.cargador = cargador;
        this.folios = folios;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva o su folio
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el folio ya está
     *         cerrado, o tiene saldo y no llegó el motivo de la autorización (RN-17 · FOL-05)
     */
    @Transactional
    public FolioResult ejecutar(CerrarFolioCommand comando) {
        Folio folio = cargador.folioDe(cargador.reserva(comando.codigoReserva()));

        comando.autorizacion(LocalDateTime.now(reloj))
                .ifPresentOrElse(autorizacion -> folio.cerrar(autorizacion), () -> folio.cerrar());

        folios.guardar(folio);
        return FolioResult.de(folio);
    }
}
