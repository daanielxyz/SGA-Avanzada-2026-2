package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador lista los apartamentos del alojamiento, activos o no, de 10 en 10 (CU-31 · DEC-18).
 */
@Service
public class ListarApartamentos {

    private final ApartamentoRepository apartamentos;

    public ListarApartamentos(ApartamentoRepository apartamentos) {
        this.apartamentos = apartamentos;
    }

    /**
     * @param numeroPagina empieza en 0
     */
    @Transactional(readOnly = true)
    public Pagina<ApartamentoResult> ejecutar(String alojamientoId, int numeroPagina) {
        return apartamentos.listarPorAlojamiento(new AlojamientoId(alojamientoId), numeroPagina)
                .map(ApartamentoResult::de);
    }
}
