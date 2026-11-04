package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data sobre la tabla {@code codigo_usuario}.
 */
public interface CodigoUsuarioRepository extends JpaRepository<CodigoUsuario, Long> {

    /** El código vigente es el último emitido: uno nuevo deja sin efecto los anteriores. */
    Optional<CodigoUsuario> findFirstByUsuarioIdAndPropositoOrderByIdDesc(String usuarioId,
                                                                          CodigoUsuario.Proposito proposito);

    long countByUsuarioIdAndPropositoAndCreadoEnAfter(String usuarioId, CodigoUsuario.Proposito proposito,
                                                      LocalDateTime desde);
}
