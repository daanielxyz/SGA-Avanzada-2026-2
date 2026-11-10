package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data sobre la tabla {@code nota_reserva}. Solo lo usa {@link NotaReservaRepositoryJpa}.
 */
interface NotaReservaJpaRepository extends JpaRepository<NotaReservaJpa, String> {

    List<NotaReservaJpa> findByReservaCodigoOrderByEscritaEnDescIdDesc(String reservaCodigo);
}
