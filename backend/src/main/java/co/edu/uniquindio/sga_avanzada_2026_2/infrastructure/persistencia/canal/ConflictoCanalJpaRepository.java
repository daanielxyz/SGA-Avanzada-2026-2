package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EstadoConflicto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data sobre la tabla {@code conflicto_canal}. Solo lo usa {@link ConflictoCanalRepositoryJpa}.
 */
interface ConflictoCanalJpaRepository extends JpaRepository<ConflictoCanalJpa, String> {

    List<ConflictoCanalJpa> findByEstadoOrderByDetectadoEnAsc(EstadoConflicto estado);
}
