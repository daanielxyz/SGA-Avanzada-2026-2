package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Un PIN del huésped, con su tabla (V12): vale 15 minutos, una sola vez y hasta el quinto intento fallido (DEC-68).
 * Solo se guarda su resumen.
 */
@Entity
@Table(name = "codigo_acceso")
public class CodigoAcceso {

    public static final int MAXIMO_INTENTOS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titularId;

    @Enumerated(EnumType.STRING)
    private PropositoCodigo proposito;

    private String pinCifrado;
    private String nuevoCorreo;
    private LocalDateTime creadoEn;
    private LocalDateTime expiraEn;
    private int intentos;
    private boolean usado;

    protected CodigoAcceso() {
    }

    private CodigoAcceso(String titularId, PropositoCodigo proposito, String pinCifrado, String nuevoCorreo,
                         LocalDateTime creadoEn, LocalDateTime expiraEn) {
        this.titularId = titularId;
        this.proposito = proposito;
        this.pinCifrado = pinCifrado;
        this.nuevoCorreo = nuevoCorreo;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    /**
     * @param pinCifrado  resumen del PIN, nunca el PIN
     * @param nuevoCorreo el correo al que se quiere cambiar; nulo si es para iniciar sesión
     */
    public static CodigoAcceso emitir(String titularId, PropositoCodigo proposito, String pinCifrado,
                                      String nuevoCorreo, LocalDateTime ahora, LocalDateTime expiraEn) {
        return new CodigoAcceso(titularId, proposito, pinCifrado, nuevoCorreo, ahora, expiraEn);
    }

    /**
     * Comprueba el PIN escrito y deja constancia: si coincide queda usado; si no, suma un intento (DEC-68). Un PIN
     * usado, vencido o con 5 intentos fallidos ya no vale, aunque sea el correcto.
     *
     * @return si el PIN vale
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

    public String nuevoCorreo() {
        return nuevoCorreo;
    }

    public int intentos() {
        return intentos;
    }

    public boolean usado() {
        return usado;
    }
}
