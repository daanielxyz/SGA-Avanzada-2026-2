package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Un PIN del huésped, con su tabla (V12): vale 15 minutos, una sola vez y hasta el quinto intento fallido (DEC-68).
 */
@Entity
@Table(name = "codigo_acceso")
public class CodigoAcceso extends CodigoDeUnSoloUso {

    private String titularId;

    @Enumerated(EnumType.STRING)
    private PropositoCodigo proposito;

    private String nuevoCorreo;

    protected CodigoAcceso() {
    }

    private CodigoAcceso(String titularId, PropositoCodigo proposito, String pinCifrado, String nuevoCorreo,
                         LocalDateTime creadoEn, LocalDateTime expiraEn) {
        super(pinCifrado, creadoEn, expiraEn);
        this.titularId = titularId;
        this.proposito = proposito;
        this.nuevoCorreo = nuevoCorreo;
    }

    /**
     * @param pinCifrado  resumen del PIN, nunca el PIN
     * @param nuevoCorreo el correo al que se quiere cambiar; nulo si es para iniciar sesión
     */
    public static CodigoAcceso emitir(String titularId, PropositoCodigo proposito, String pinCifrado,
                                      String nuevoCorreo, LocalDateTime ahora, LocalDateTime expiraEn) {
        return new CodigoAcceso(titularId, proposito, pinCifrado, nuevoCorreo, ahora, expiraEn);
    }

    public String nuevoCorreo() {
        return nuevoCorreo;
    }
}
