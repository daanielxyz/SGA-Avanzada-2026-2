package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Un token cuya sesión se cerró antes de vencer (CU-48 · DEC-71). Se identifica por su {@code jti}.
 */
@Entity
@Table(name = "token_revocado")
public class TokenRevocado {

    @Id
    private String jti;

    private Instant expiraEn;

    protected TokenRevocado() {
    }

    public TokenRevocado(String jti, Instant expiraEn) {
        this.jti = jti;
        this.expiraEn = expiraEn;
    }
}
