package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link PoliticaCancelacionRepository} (DEC-13).
 */
@Repository
@Transactional
public class PoliticaCancelacionRepositoryJpa implements PoliticaCancelacionRepository {

    private final PoliticaCancelacionJpaRepository filas;

    public PoliticaCancelacionRepositoryJpa(PoliticaCancelacionJpaRepository filas) {
        this.filas = filas;
    }

    /**
     * Inserta una versión nueva. Una versión ya guardada nunca se sobrescribe (POL-04 · POL-07).
     *
     * @throws ReglaDominioException si ya existe una política con ese id
     */
    @Override
    public void guardar(PoliticaCancelacion politica) {
        if (filas.existsById(politica.id().valor())) {
            throw new ReglaDominioException("La versión " + politica.id().valor() + " ya existe y no se modifica");
        }
        filas.save(PoliticaCancelacionMapper.aJpa(politica));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PoliticaCancelacion> buscarPorId(PoliticaId id) {
        return filas.findById(id.valor()).map(PoliticaCancelacionMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PoliticaCancelacion> buscarVigente(AlojamientoId alojamientoId) {
        return filas.findFirstByAlojamientoIdOrderByVersionDesc(alojamientoId.valor())
                .map(PoliticaCancelacionMapper::aDominio);
    }
}
