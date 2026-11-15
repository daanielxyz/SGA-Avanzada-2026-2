package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EstadoEnvioTra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data sobre la tabla {@code envio_tra}. Solo lo usa {@link EnvioTraRepositoryJpa}.
 */
interface EnvioTraJpaRepository extends JpaRepository<EnvioTraJpa, String> {

    @Query("select e.reservaCodigo from EnvioTraJpa e where e.estado = :pendiente "
            + "or (e.estado = :conError and e.intentos < :maximoIntentos) order by e.reservaCodigo")
    List<String> buscarParaEnviar(@Param("pendiente") EstadoEnvioTra pendiente,
                                  @Param("conError") EstadoEnvioTra conError,
                                  @Param("maximoIntentos") int maximoIntentos);

    Page<EnvioTraJpa> findByEstadoOrderByUltimoIntentoDescReservaCodigoDesc(EstadoEnvioTra estado, Pageable pagina);
}
