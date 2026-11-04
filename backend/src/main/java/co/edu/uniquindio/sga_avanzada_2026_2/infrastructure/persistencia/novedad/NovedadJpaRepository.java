package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.novedad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data sobre la tabla {@code novedad}. Solo lo usa {@link NovedadRepositoryJpa}.
 */
interface NovedadJpaRepository extends JpaRepository<NovedadJpa, String> {

    List<NovedadJpa> findByApartamentoCodigo(String apartamentoCodigo);
}
