package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data sobre la tabla {@code titular}. Solo lo usa {@link TitularRepositoryJpa}.
 */
interface TitularJpaRepository extends JpaRepository<TitularJpa, String> {

    Optional<TitularJpa> findByAlojamientoIdAndTipoDocumentoAndNumeroDocumento(String alojamientoId,
                                                                              TipoDocumento tipo, String numero);
}
