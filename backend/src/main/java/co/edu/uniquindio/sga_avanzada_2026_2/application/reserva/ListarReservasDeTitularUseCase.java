package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el huésped ve sus reservas en su cuenta (USU-04 · DEC-68): todas, de la más reciente a la más antigua,
 * o solo las vigentes o las terminadas, cada grupo en el orden en que se buscan (DEC-108).
 */
@Service
public class ListarReservasDeTitularUseCase {

    private final ReservaRepository reservas;

    public ListarReservasDeTitularUseCase(ReservaRepository reservas) {
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

    /**
     * @param vigentes {@code true}: PENDIENTE, CONFIRMADA y EN_CURSO, de la próxima llegada a la más lejana;
     *                 {@code false}: las terminadas, de la estancia más reciente a la más antigua (DEC-108)
     */
    @Transactional(readOnly = true)
    public Pagina<ReservaResult> ejecutar(String titularId, boolean vigentes, int pagina) {
        return reservas.listarPorTitular(new TitularId(titularId), vigentes, pagina).map(ReservaResult::de);
    }
}
