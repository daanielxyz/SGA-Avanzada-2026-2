package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: ver el detalle de un apartamento por su código.
 */
@Service
public class ConsultarApartamento {

    private final ApartamentoRepository apartamentos;

    public ConsultarApartamento(ApartamentoRepository apartamentos) {
        this.apartamentos = apartamentos;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public ApartamentoResult ejecutar(String codigo) {
        ApartamentoId id = new ApartamentoId(codigo);
        return apartamentos.buscarPorCodigo(id)
                .map(ApartamentoResult::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));
    }
}
