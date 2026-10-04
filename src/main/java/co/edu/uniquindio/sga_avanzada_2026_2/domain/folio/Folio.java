package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;

import java.util.List;

/**
 * Raíz del agregado Folio. Saldo = cargos − pagos (calculado, no se guarda).
 */
public class Folio {

    private final FolioId id;
    private final ReservaId reservaId;
    private List<Cargo> cargos;
    private List<Pago> pagos;
    private boolean cerrado;
    private AutorizacionCierre autorizacion; // opcional

    public Folio(FolioId id, ReservaId reservaId, List<Cargo> cargos, List<Pago> pagos, boolean cerrado,
                 AutorizacionCierre autorizacion) {
        this.id = id;
        this.reservaId = reservaId;
        this.cargos = List.copyOf(cargos);
        this.pagos = List.copyOf(pagos);
        this.cerrado = cerrado;
        this.autorizacion = autorizacion;
    }
}
