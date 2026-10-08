package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.apartamento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data sobre la tabla {@code apartamento}. Solo lo usa {@link ApartamentoRepositoryJpa}.
 */
interface ApartamentoJpaRepository extends JpaRepository<ApartamentoJpa, String> {

    Page<ApartamentoJpa> findByAlojamientoIdOrderByCodigo(String alojamientoId, Pageable pagina);

    List<ApartamentoJpa> findByAlojamientoIdAndActivoTrue(String alojamientoId);
}
