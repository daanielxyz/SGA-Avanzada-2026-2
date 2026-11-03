package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: todas las versiones de las tarifas de un apartamento, de la más reciente a la más antigua y de 10 en 10
 * (CU-34 · TAR-05 · DEC-18).
 */
@Service
public class ConsultarHistorialTarifasUseCase {

    private final TarifaRepository tarifas;

    public ConsultarHistorialTarifasUseCase(TarifaRepository tarifas) {
        this.tarifas = tarifas;
    }

    @Transactional(readOnly = true)
    public Pagina<TarifaResult> ejecutar(String apartamento, int numeroPagina) {
        return tarifas.listarHistorial(new ApartamentoId(apartamento), numeroPagina).map(TarifaResult::de);
    }
}
