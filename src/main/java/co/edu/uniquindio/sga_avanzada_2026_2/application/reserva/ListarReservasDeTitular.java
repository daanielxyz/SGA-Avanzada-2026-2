package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el huésped ve sus reservas en su cuenta, de la más reciente a la más antigua (USU-04 · DEC-68).
 */
@Service
public class ListarReservasDeTitular {

    private final ReservaRepository reservas;

    public ListarReservasDeTitular(ReservaRepository reservas) {
        this.reservas = reservas;
    }

    /**
     * @param titularId el del huésped que inició sesión, nunca uno que llegue en la petición
     * @param pagina    empieza en 0 (DEC-18)
     */
    @Transactional(readOnly = true)
    public Pagina<ReservaResult> ejecutar(String titularId, int pagina) {
        return reservas.listarPorTitular(new TitularId(titularId), pagina).map(ReservaResult::de);
    }
}
