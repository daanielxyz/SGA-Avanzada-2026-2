package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data sobre la tabla {@code codigo_acceso}.
 */
public interface CodigoAccesoRepository extends JpaRepository<CodigoAcceso, Long> {

    /** El PIN vigente es el último emitido: uno nuevo deja sin efecto los anteriores. */
    Optional<CodigoAcceso> findFirstByTitularIdAndPropositoOrderByIdDesc(String titularId, PropositoCodigo proposito);

    long countByTitularIdAndPropositoAndCreadoEnAfter(String titularId, PropositoCodigo proposito,
                                                      LocalDateTime desde);
}
