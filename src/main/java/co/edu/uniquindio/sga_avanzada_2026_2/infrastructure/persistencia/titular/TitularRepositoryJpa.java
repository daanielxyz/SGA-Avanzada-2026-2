package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link TitularRepository} (DEC-13).
 */
@Repository
@Transactional
public class TitularRepositoryJpa implements TitularRepository {

    private final TitularJpaRepository filas;

    public TitularRepositoryJpa(TitularJpaRepository filas) {
        this.filas = filas;
    }

    /** Inserta o actualiza reutilizando la fila cargada, para conservar su {@code version} (DEC-17). */
    @Override
    public void guardar(Titular titular) {
        TitularJpa fila = filas.findById(titular.id().valor()).orElseGet(TitularJpa::new);
        TitularMapper.copiar(titular, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Titular> buscarPorId(TitularId id) {
        return filas.findById(id.valor()).map(TitularMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Titular> buscarPorDocumento(AlojamientoId alojamientoId, Documento documento) {
        return filas.findByAlojamientoIdAndTipoDocumentoAndNumeroDocumento(alojamientoId.valor(), documento.tipo(),
                        documento.numero())
                .map(TitularMapper::aDominio);
    }
}
