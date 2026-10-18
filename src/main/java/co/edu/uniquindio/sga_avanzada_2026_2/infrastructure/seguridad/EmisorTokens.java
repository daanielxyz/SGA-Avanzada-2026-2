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
import java.util.Optional;
import java.util.UUID;

/**
 * Emite el token JWT de quien inicia sesión (CU-02): firmado con HS256, con su id como sujeto y su rol (DEC-67). El
 * del personal interno lleva el id de su usuario; el del huésped, el de su titular (DEC-68).
 */
@Component
public class EmisorTokens {

    /** Nombre del dato del token que lleva el rol. */
    public static final String ROL = "rol";

    /** Dato del token del personal con la versión de sus sesiones (DEC-71). */
    public static final String SESIONES = "sesiones";

    private final JwtEncoder codificador;
    private final Clock reloj;
    private final Duration vigencia;

    public EmisorTokens(JwtEncoder codificador, Clock reloj, SeguridadPropiedades propiedades) {
        this.codificador = codificador;
        this.reloj = reloj;
        this.vigencia = Duration.ofMinutes(propiedades.jwt().duracionMinutos());
    }

    public SesionIniciada emitir(Usuario usuario) {
        return emitir(usuario.id(), usuario.nombre(), usuario.rol(), (long) usuario.versionSesiones());
    }

    /** El del huésped: su titular no tiene versión de sesiones; las cierra con el cierre de sesión (DEC-71). */
    public SesionIniciada emitir(String id, String nombre, Rol rol) {
        return emitir(id, nombre, rol, null);
    }

    private SesionIniciada emitir(String id, String nombre, Rol rol, Long versionSesiones) {
        Instant ahora = reloj.instant();
        Instant expiraEn = ahora.plus(vigencia);
        JwtClaimsSet.Builder datos = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString()) // para poder cerrar esta sesión (CU-48 · DEC-71)
                .subject(id)
                .claim(ROL, rol.name())
                .issuedAt(ahora)
                .expiresAt(expiraEn);
        Optional.ofNullable(versionSesiones).ifPresent(version -> datos.claim(SESIONES, version));
        String token = codificador
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), datos.build()))
                .getTokenValue();
        return new SesionIniciada(token, expiraEn, id, nombre, rol);
    }
}
