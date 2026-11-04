package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

/**
 * Spring Data sobre la tabla {@code token_revocado}.
 */
public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, String> {

    /** Los tokens ya vencidos no hace falta recordarlos: no valen de todos modos. */
    @Modifying
    @Query("delete from TokenRevocado t where t.expiraEn < :ahora")
    int borrarVencidos(Instant ahora);
}
