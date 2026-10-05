package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.apartamento;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data sobre la tabla {@code apartamento}. Solo lo usa {@link ApartamentoRepositoryJpa}.
 */
interface ApartamentoJpaRepository extends JpaRepository<ApartamentoJpa, String> {
}
