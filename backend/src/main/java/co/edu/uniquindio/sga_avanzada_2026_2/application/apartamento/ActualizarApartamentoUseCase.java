package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador corrige los datos de un apartamento (CU-31 · CARAC-01).
 */
@Service
public class ActualizarApartamentoUseCase {

    private final ApartamentoRepository apartamentos;

    public ActualizarApartamentoUseCase(ApartamentoRepository apartamentos) {
        this.apartamentos = apartamentos;
    }

    /**
     * @throws RecursoNoEncontradoException si el apartamento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si los datos violan una invariante del apartamento
     */
    @Transactional
    public ApartamentoResult ejecutar(ActualizarApartamentoCommand comando) {
        ApartamentoId id = new ApartamentoId(comando.codigo());
        Apartamento apartamento = apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));

        apartamento.actualizarDatos(comando.nombre(), comando.descripcion(), new Dormitorio(comando.dormitorios()),
                comando.caracteristicas().stream().map(Caracteristica::new).toList());

        apartamentos.guardar(apartamento);
        return ApartamentoResult.de(apartamento);
    }
}
