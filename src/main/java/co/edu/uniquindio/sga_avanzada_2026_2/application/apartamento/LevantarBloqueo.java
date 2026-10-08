package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.BloqueoId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador levanta un bloqueo y sus noches quedan libres de inmediato (CU-52 · BLO-06).
 */
@Service
public class LevantarBloqueo {

    private final ApartamentoRepository apartamentos;

    public LevantarBloqueo(ApartamentoRepository apartamentos) {
        this.apartamentos = apartamentos;
    }

    /**
     * @throws RecursoNoEncontradoException si el apartamento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el bloqueo no es de
     *                                      este apartamento o ya fue levantado
     */
    @Transactional
    public ApartamentoResult ejecutar(LevantarBloqueoCommand comando) {
        ApartamentoId id = new ApartamentoId(comando.codigo());
        Apartamento apartamento = apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));

        apartamento.levantarBloqueo(new BloqueoId(comando.bloqueoId()));

        apartamentos.guardar(apartamento);
        return ApartamentoResult.de(apartamento);
    }
}
