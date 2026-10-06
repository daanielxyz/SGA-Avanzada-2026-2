package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Spring Data sobre la tabla {@code reserva}. Solo lo usa {@link ReservaRepositoryJpa}.
 */
interface ReservaJpaRepository extends JpaRepository<ReservaJpa, String> {

    List<ReservaJpa> findByApartamentoCodigoAndEstadoIn(String apartamentoCodigo, Collection<EstadoReserva> estados);
}
