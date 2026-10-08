package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso: ver las tarifas vigentes hoy de un apartamento, una por temporada (CU-34 · TAR-06).
 */
@Service
public class ConsultarTarifas {

    private final TarifaRepository tarifas;
    private final Clock reloj;

    public ConsultarTarifas(TarifaRepository tarifas, Clock reloj) {
        this.tarifas = tarifas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<TarifaResult> ejecutar(String apartamento) {
        return tarifas.buscarVigentesPorApartamento(new ApartamentoId(apartamento), LocalDate.now(reloj)).stream()
                .map(TarifaResult::de)
                .toList();
    }
}
