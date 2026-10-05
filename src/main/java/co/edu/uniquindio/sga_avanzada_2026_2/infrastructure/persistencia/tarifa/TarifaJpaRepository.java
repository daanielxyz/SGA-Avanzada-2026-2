package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tarifa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data sobre la tabla {@code tarifa}. Solo lo usa {@link TarifaRepositoryJpa}.
 */
interface TarifaJpaRepository extends JpaRepository<TarifaJpa, String> {

    /**
     * Para cada temporada, la fila con la versión más alta del apartamento cuya vigencia ya empezó en {@code fecha}.
     */
    @Query("""
            select t from TarifaJpa t
            where t.apartamentoCodigo = :apartamento
              and t.vigenteDesde <= :fecha
              and t.version = (select max(t2.version) from TarifaJpa t2
                               where t2.apartamentoCodigo = t.apartamentoCodigo
                                 and t2.temporadaId = t.temporadaId
                                 and t2.vigenteDesde <= :fecha)
            """)
    List<TarifaJpa> buscarVigentes(@Param("apartamento") String apartamentoCodigo, @Param("fecha") LocalDate fecha);
}
