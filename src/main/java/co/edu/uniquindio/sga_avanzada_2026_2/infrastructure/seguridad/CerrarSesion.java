package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Cierre de sesión (CU-48): el token de la petición deja de valer aunque no haya vencido. Vale para el personal y
 * para el huésped (DEC-71).
 */
@Service
public class CerrarSesion {

    private final TokenRevocadoRepository revocados;
    private final Clock reloj;

    public CerrarSesion(TokenRevocadoRepository revocados, Clock reloj) {
        this.revocados = revocados;
        this.reloj = reloj;
    }

    /** Un token sin identificador ({@code jti}) no se puede cerrar; de paso olvida los cerrados que ya vencieron. */
    @Transactional
    public void ejecutar(Jwt token) {
        revocados.borrarVencidos(reloj.instant());
        if (token.getId() != null) {
            revocados.save(new TokenRevocado(token.getId(), token.getExpiresAt()));
        }
    }
}
