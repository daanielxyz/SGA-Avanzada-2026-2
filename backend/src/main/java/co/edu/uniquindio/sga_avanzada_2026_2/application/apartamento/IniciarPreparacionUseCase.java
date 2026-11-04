package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el personal empieza a preparar un apartamento (CU-42 · EOPE-02). Quién puede hacerlo lo controla la seguridad (DEC-22 · DEC-67).
 */
@Service
public class IniciarPreparacionUseCase {

    private final ApartamentoRepository apartamentos;

    public IniciarPreparacionUseCase(ApartamentoRepository apartamentos) {
        this.apartamentos = apartamentos;
    }

    /**
     * @throws RecursoNoEncontradoException si el apartamento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la transición no está permitida (EOPE-02)
     */
    @Transactional
    public ApartamentoResult ejecutar(String codigo) {
        ApartamentoId id = new ApartamentoId(codigo);
        Apartamento apartamento = apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));

        apartamento.iniciarPreparacion();

        apartamentos.guardar(apartamento);
        return ApartamentoResult.de(apartamento);
    }
}
