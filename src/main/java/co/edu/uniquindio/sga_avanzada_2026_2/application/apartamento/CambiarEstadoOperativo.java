package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: avanzar la preparación de un apartamento (CU-42) o declararlo fuera de servicio (CU-40). Que solo el
 * administrador declare FUERA_DE_SERVICIO (EOPE-04) lo controla la seguridad (B3 · DEC-22).
 */
@Service
public class CambiarEstadoOperativo {

    private final ApartamentoRepository apartamentos;

    public CambiarEstadoOperativo(ApartamentoRepository apartamentos) {
        this.apartamentos = apartamentos;
    }

    /**
     * @throws RecursoNoEncontradoException si el apartamento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la transición no
     *                                      está permitida (EOPE-02)
     */
    @Transactional
    public ApartamentoResult ejecutar(CambiarEstadoOperativoCommand comando) {
        ApartamentoId id = new ApartamentoId(comando.codigo());
        Apartamento apartamento = apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));

        apartamento.cambiarEstadoOperativo(EstadoOperativo.valueOf(comando.estado()));

        apartamentos.guardar(apartamento);
        return ApartamentoResult.de(apartamento);
    }
}
