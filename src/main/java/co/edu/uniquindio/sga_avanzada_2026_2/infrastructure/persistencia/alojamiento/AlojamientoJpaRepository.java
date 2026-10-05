package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.alojamiento;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data sobre la tabla {@code alojamiento}. Solo lo usa {@link AlojamientoRepositoryJpa}.
 */
interface AlojamientoJpaRepository extends JpaRepository<AlojamientoJpa, String> {
}
