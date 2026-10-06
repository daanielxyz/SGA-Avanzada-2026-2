package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.math.BigDecimal;

/**
 * VO calculado: saldo del folio como monto positivo y situación, porque {@link Dinero} nunca es negativo (SLD-03 ·
 * DEC-20). Nunca se guarda (SLD-01).
 */
public record Saldo(Dinero monto, SituacionSaldo situacion) {

    public Saldo {
        if (monto == null || situacion == null) {
            throw new ReglaDominioException("El saldo requiere monto y situación");
        }
        if (monto.esCero() != (situacion == SituacionSaldo.AL_DIA)) {
            throw new ReglaDominioException("Solo un saldo en cero está al día");
        }
    }

    /**
     * Traduce la diferencia cargos − pagos netos: positiva = debe el huésped, negativa = tiene a favor (SLD-03).
     */
    static Saldo de(BigDecimal diferencia) {
        SituacionSaldo situacion = switch (diferencia.signum()) {
            case 1 -> SituacionSaldo.PENDIENTE;
            case -1 -> SituacionSaldo.A_FAVOR;
            default -> SituacionSaldo.AL_DIA;
        };
        return new Saldo(new Dinero(diferencia.abs()), situacion);
    }

    public boolean alDia() {
        return situacion == SituacionSaldo.AL_DIA;
    }
}
