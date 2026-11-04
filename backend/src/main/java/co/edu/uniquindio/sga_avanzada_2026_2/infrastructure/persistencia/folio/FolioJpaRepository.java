package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.folio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data sobre la tabla {@code folio}. Solo lo usa {@link FolioRepositoryJpa}.
 */
interface FolioJpaRepository extends JpaRepository<FolioJpa, String> {

    Optional<FolioJpa> findByReservaCodigo(String reservaCodigo);
}
