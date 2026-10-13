package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Emite el token JWT de un usuario (CU-02): firmado con HS256, con su id como sujeto y su rol (DEC-67).
 */
@Component
public class EmisorTokens {

    /** Nombre del dato del token que lleva el rol. */
    public static final String ROL = "rol";

    private final JwtEncoder codificador;
    private final Clock reloj;
    private final Duration vigencia;

    public EmisorTokens(JwtEncoder codificador, Clock reloj, SeguridadPropiedades propiedades) {
        this.codificador = codificador;
        this.reloj = reloj;
        this.vigencia = Duration.ofMinutes(propiedades.jwt().duracionMinutos());
    }

    public SesionIniciada emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant expiraEn = ahora.plus(vigencia);
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .subject(usuario.id())
                .claim(ROL, usuario.rol().name())
                .issuedAt(ahora)
                .expiresAt(expiraEn)
                .build();
        String token = codificador
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), datos))
                .getTokenValue();
        return new SesionIniciada(token, expiraEn, usuario.id(), usuario.nombre(), usuario.rol());
    }
}
