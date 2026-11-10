package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data sobre la tabla {@code reserva}. Solo lo usa {@link ReservaRepositoryJpa}.
 */
interface ReservaJpaRepository extends JpaRepository<ReservaJpa, String>, JpaSpecificationExecutor<ReservaJpa> {

    List<ReservaJpa> findByApartamentoCodigoAndEstadoIn(String apartamentoCodigo, Collection<EstadoReserva> estados);

    @Query("select r from ReservaJpa r where r.apartamentoCodigo = :apartamento and r.estado in :estados"
            + " and r.entrada < :hasta and r.salida > :desde")
    List<ReservaJpa> buscarEnRango(@Param("apartamento") String apartamento,
                                   @Param("estados") Collection<EstadoReserva> estados,
                                   @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select r from ReservaJpa r where r.estado in :estados and (r.carros > 0 or r.motos > 0)")
    List<ReservaJpa> buscarConVehiculos(@Param("estados") Collection<EstadoReserva> estados);

    Optional<ReservaJpa> findByCanalIdAndIdExterno(String canalId, String idExterno);

    List<ReservaJpa> findByEstadoAndCreadaEnBeforeOrderByCreadaEn(EstadoReserva estado, LocalDateTime limite);

    Page<ReservaJpa> findByTitularIdOrderByCreadaEnDescCodigoDesc(String titularId, Pageable pagina);
}
