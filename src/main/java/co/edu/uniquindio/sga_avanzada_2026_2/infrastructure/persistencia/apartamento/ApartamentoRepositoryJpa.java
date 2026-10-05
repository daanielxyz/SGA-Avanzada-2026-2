package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link ApartamentoRepository} (DEC-13).
 */
@Repository
@Transactional
public class ApartamentoRepositoryJpa implements ApartamentoRepository {

    private final ApartamentoJpaRepository filas;

    public ApartamentoRepositoryJpa(ApartamentoJpaRepository filas) {
        this.filas = filas;
    }

    /**
     * Inserta o actualiza. Si la fila ya existe se reutiliza para conservar su {@code version}: dentro de la
     * transacción del caso de uso es la misma instancia que se cargó, y un cambio concurrente provoca un
     * conflicto de bloqueo optimista en lugar de sobrescribirse (DEC-17).
     */
    @Override
    public void guardar(Apartamento apartamento) {
        ApartamentoJpa fila = filas.findById(apartamento.codigo().valor()).orElseGet(ApartamentoJpa::new);
        ApartamentoMapper.copiar(apartamento, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Apartamento> buscarPorCodigo(ApartamentoId codigo) {
        return filas.findById(codigo.valor()).map(ApartamentoMapper::aDominio);
    }
}
