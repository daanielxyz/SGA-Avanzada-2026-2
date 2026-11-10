package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link FolioRepository} (DEC-13).
 */
@Repository
@Transactional
public class FolioRepositoryJpa implements FolioRepository {

    private final FolioJpaRepository filas;

    public FolioRepositoryJpa(FolioJpaRepository filas) {
        this.filas = filas;
    }

    /**
     * Inserta o actualiza reutilizando la fila cargada, para conservar su {@code version} (DEC-17): dos pagos
     * concurrentes sobre el mismo folio dan conflicto en vez de perder uno.
     */
    @Override
    public void guardar(Folio folio) {
        FolioJpa fila = filas.findById(folio.id().valor()).orElseGet(FolioJpa::new);
        FolioMapper.copiar(folio, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folio> buscarPorReserva(ReservaId reservaId) {
        return filas.findByReservaCodigo(reservaId.valor()).map(FolioMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folio> buscarPorReservas(Collection<ReservaId> reservaIds) {
        return filas.findByReservaCodigoIn(reservaIds.stream().map(ReservaId::valor).toList()).stream()
                .map(FolioMapper::aDominio)
                .toList();
    }
}
