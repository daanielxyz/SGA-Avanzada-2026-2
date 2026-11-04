package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Un código del personal interno, con su tabla (V14): para fijar la contraseña al activar la cuenta o al recuperarla
 * (DEC-71 · USU-06).
 */
@Entity
@Table(name = "codigo_usuario")
public class CodigoUsuario extends CodigoDeUnSoloUso {

    /** Para qué sirve el código y cuánto vale. */
    public enum Proposito {
        /** El usuario recién creado fija su primera contraseña (DEC-71). */
        ACTIVACION(Duration.ofHours(24)),
        /** El usuario olvidó la contraseña (USU-06). */
        RECUPERACION(Duration.ofMinutes(15));

        private final Duration vigencia;

        Proposito(Duration vigencia) {
            this.vigencia = vigencia;
        }

        public Duration vigencia() {
            return vigencia;
        }
    }

    private String usuarioId;

    @Enumerated(EnumType.STRING)
    private Proposito proposito;

    protected CodigoUsuario() {
    }

    private CodigoUsuario(String usuarioId, Proposito proposito, String pinCifrado, LocalDateTime ahora) {
        super(pinCifrado, ahora, ahora.plus(proposito.vigencia()));
        this.usuarioId = usuarioId;
        this.proposito = proposito;
    }

    /** @param pinCifrado resumen del código, nunca el código */
    public static CodigoUsuario emitir(String usuarioId, Proposito proposito, String pinCifrado, LocalDateTime ahora) {
        return new CodigoUsuario(usuarioId, proposito, pinCifrado, ahora);
    }
}
