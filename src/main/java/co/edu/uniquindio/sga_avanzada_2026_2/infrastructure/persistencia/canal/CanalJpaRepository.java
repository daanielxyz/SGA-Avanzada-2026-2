package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data sobre la tabla {@code canal}. Solo lo usa {@link CanalRepositoryJpa}.
 */
interface CanalJpaRepository extends JpaRepository<CanalJpa, String> {
}
