package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link AlojamientoRepository} (DEC-13).
 */
@Repository
@Transactional
public class AlojamientoRepositoryJpa implements AlojamientoRepository {

    private final AlojamientoJpaRepository filas;

    public AlojamientoRepositoryJpa(AlojamientoJpaRepository filas) {
        this.filas = filas;
    }

    /**
     * Inserta o actualiza reutilizando la fila cargada para conservar su {@code version} (DEC-17).
     */
    @Override
    public void guardar(Alojamiento alojamiento) {
        AlojamientoJpa fila = filas.findById(alojamiento.id().valor()).orElseGet(AlojamientoJpa::new);
        AlojamientoMapper.copiar(alojamiento, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alojamiento> buscarPorId(AlojamientoId id) {
        return filas.findById(id.valor()).map(AlojamientoMapper::aDominio);
    }
}
