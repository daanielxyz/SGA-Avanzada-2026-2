package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

/**
 * Lectura del saldo del folio (SLD-03): el huésped debe, tiene a favor o está al día.
 */
public enum SituacionSaldo {
    PENDIENTE("pendiente"),
    A_FAVOR("a favor del huésped"),
    AL_DIA("al día");

    private final String texto;

    SituacionSaldo(String texto) {
        this.texto = texto;
    }

    /** Cómo se nombra en un mensaje: «un saldo pendiente de $ 50.000» (#8 · DEC-118). */
    public String texto() {
        return texto;
    }
}
