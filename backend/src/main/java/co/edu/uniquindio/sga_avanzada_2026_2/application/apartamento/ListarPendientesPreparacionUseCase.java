package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el personal de servicio consulta qué apartamentos tiene por preparar, de 10 en 10 (CU-41 · DEC-18).
 */
@Service
public class ListarPendientesPreparacionUseCase {

    private final ApartamentoRepository apartamentos;

    public ListarPendientesPreparacionUseCase(ApartamentoRepository apartamentos) {
        this.apartamentos = apartamentos;
    }

    @Transactional(readOnly = true)
    public Pagina<ApartamentoResult> ejecutar(String alojamientoId, int numeroPagina) {
        return Pagina.de(apartamentos.buscarActivos(new AlojamientoId(alojamientoId)).stream()
                .filter(Apartamento::pendienteDePreparacion)
                .map(ApartamentoResult::de)
                .toList(), numeroPagina);
    }
}
