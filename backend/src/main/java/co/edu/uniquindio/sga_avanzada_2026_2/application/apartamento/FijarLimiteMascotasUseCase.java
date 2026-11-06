package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador fija cuántas mascotas admite un apartamento por reserva, o lo devuelve al límite del
 * alojamiento (RP-03 · DEC-93). Las reservas ya creadas no cambian.
 */
@Service
public class FijarLimiteMascotasUseCase {

    private final ApartamentoRepository apartamentos;

    public FijarLimiteMascotasUseCase(ApartamentoRepository apartamentos) {
        this.apartamentos = apartamentos;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el apartamento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el límite es negativo
     */
    @Transactional
    public ApartamentoResult ejecutar(FijarLimiteMascotasCommand comando) {
        ApartamentoId id = new ApartamentoId(comando.codigo());
        Apartamento apartamento = apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));
        apartamento.fijarLimiteMascotas(comando.limiteMascotas());
        apartamentos.guardar(apartamento);
        return ApartamentoResult.de(apartamento);
    }
}
