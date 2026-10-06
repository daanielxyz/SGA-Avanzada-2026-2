package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.politica;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data sobre la tabla {@code politica_cancelacion}. Solo lo usa {@link PoliticaCancelacionRepositoryJpa}.
 */
interface PoliticaCancelacionJpaRepository extends JpaRepository<PoliticaCancelacionJpa, String> {

    Optional<PoliticaCancelacionJpa> findFirstByAlojamientoIdOrderByVersionDesc(String alojamientoId);
}
