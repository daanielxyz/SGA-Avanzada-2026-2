package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.ClavesReserva;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Claves de reserva guardadas como huella SHA-256 (DEC-93). Cada clave son 32 bytes al azar, imposibles de adivinar,
 * así que basta una huella rápida; las contraseñas y los PIN, que son cortos, usan BCrypt.
 */
@Component
public class ClavesReservaJpa implements ClavesReserva {

    private static final int BYTES_CLAVE = 32;
    private static final SecureRandom AZAR = new SecureRandom();

    private final ClaveReservaRepository claves;
    private final Clock reloj;

    public ClavesReservaJpa(ClaveReservaRepository claves, Clock reloj) {
        this.claves = claves;
        this.reloj = reloj;
    }

    @Override
    public String emitir(String codigoReserva) {
        byte[] azar = new byte[BYTES_CLAVE];
        AZAR.nextBytes(azar);
        String clave = Base64.getUrlEncoder().withoutPadding().encodeToString(azar);
        claves.save(new ClaveReserva(codigoReserva, huella(clave), LocalDateTime.now(reloj)));
        return clave;
    }

    @Override
    public boolean corresponde(String codigoReserva, String clave) {
        // Comparación en tiempo constante: el tiempo de respuesta no dice cuánto de la clave coincide
        return claves.findById(codigoReserva)
                .map(guardada -> MessageDigest.isEqual(guardada.huella().getBytes(StandardCharsets.US_ASCII),
                        huella(clave).getBytes(StandardCharsets.US_ASCII)))
                .orElse(false);
    }

    private static String huella(String clave) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(clave.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
