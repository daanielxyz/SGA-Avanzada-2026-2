package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tarifa;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data sobre la tabla {@code calendario_temporadas}. Solo lo usa {@link CalendarioTemporadasRepositoryJpa}.
 */
interface CalendarioTemporadasJpaRepository extends JpaRepository<CalendarioTemporadasJpa, String> {
}
