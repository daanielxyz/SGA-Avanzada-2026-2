package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.Novedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link NovedadRepository} (DEC-13). No borra filas (NOV-06).
 */
@Repository
@Transactional
public class NovedadRepositoryJpa implements NovedadRepository {

    private final NovedadJpaRepository filas;

    public NovedadRepositoryJpa(NovedadJpaRepository filas) {
        this.filas = filas;
    }

    /** Inserta o actualiza reutilizando la fila cargada, para conservar su {@code version} (DEC-17). */
    @Override
    public void guardar(Novedad novedad) {
        NovedadJpa fila = filas.findById(novedad.id().valor()).orElseGet(NovedadJpa::new);
        NovedadMapper.copiar(novedad, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Novedad> buscarPorId(NovedadId id) {
        return filas.findById(id.valor()).map(NovedadMapper::aDominio);
    }

    /** La fecha de registro vive en el historial, así que el orden se aplica al cargar. */
    @Override
    @Transactional(readOnly = true)
    public List<Novedad> buscarPorApartamento(ApartamentoId apartamentoId) {
        return filas.findByApartamentoCodigo(apartamentoId.valor()).stream()
                .map(NovedadMapper::aDominio)
                .sorted(Comparator.comparing(Novedad::registradaEn).reversed())
                .toList();
    }
}
