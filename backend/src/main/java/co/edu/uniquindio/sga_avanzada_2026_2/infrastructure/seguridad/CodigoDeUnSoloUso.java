package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Lo común a los códigos que llegan por correo o SMS: el PIN del huésped (DEC-69) y los códigos del personal (DEC-71).
 * Vale hasta su vencimiento, una sola vez y hasta el quinto intento fallido; solo se guarda su resumen.
 */
@MappedSuperclass
public abstract class CodigoDeUnSoloUso {

    public static final int MAXIMO_INTENTOS = 5;

    private static final SecureRandom AZAR = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pinCifrado;
    private LocalDateTime creadoEn;
    private LocalDateTime expiraEn;
    private int intentos;
    private boolean usado;

    protected CodigoDeUnSoloUso() {
    }

    protected CodigoDeUnSoloUso(String pinCifrado, LocalDateTime creadoEn, LocalDateTime expiraEn) {
        this.pinCifrado = pinCifrado;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    /** Un código nuevo de 6 dígitos, al azar. */
    public static String nuevoPin() {
        return "%06d".formatted(AZAR.nextInt(1_000_000));
    }

    /**
     * Comprueba el código escrito y deja constancia: si coincide queda usado; si no, suma un intento. Uno usado,
     * vencido o con 5 intentos fallidos ya no vale, aunque sea el correcto.
     *
     * @return si el código vale
     */
    public boolean verificar(String pin, LocalDateTime ahora, PasswordEncoder cifrador) {
        if (usado || !ahora.isBefore(expiraEn) || intentos >= MAXIMO_INTENTOS) {
            return false;
        }
        if (cifrador.matches(pin, pinCifrado)) {
            usado = true;
            return true;
        }
        intentos++;
        return false;
    }

    public int intentos() {
        return intentos;
    }

    public boolean usado() {
        return usado;
    }
}
