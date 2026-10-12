package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

/**
 * Spring Data sobre la tabla {@code evento_canal}. Solo lo usa {@link EventoCanalRepositoryJpa}.
 */
interface EventoCanalJpaRepository extends JpaRepository<EventoCanalJpa, String> {

    Page<EventoCanalJpa> findByCanalIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(
            String canalId, LocalDateTime desde, LocalDateTime hasta, Pageable pagina);
}
