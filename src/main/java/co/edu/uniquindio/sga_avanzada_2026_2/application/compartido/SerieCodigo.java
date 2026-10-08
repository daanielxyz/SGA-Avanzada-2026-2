package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

import java.time.LocalDate;

/**
 * Series de códigos de negocio que el sistema genera (DEC-52): {@code PREFIJO-n}, salvo la reserva, que lleva el año
 * ({@code RES-2026-00042}, DEC-04). Los movimientos del folio y los ocupantes no están aquí porque los numera su
 * propio agregado (DEC-43 · DEC-56).
 */
public enum SerieCodigo {
    SERVICIO("SRV"),
    POLITICA("POL"),
    TEMPORADA("TEM"),
    TARIFA("TAR"),
    BLOQUEO("BLO"),
    TITULAR("TIT"),
    FOLIO("FOL"),
    RESERVA("RES") {
        @Override
        public String formatear(long numero, LocalDate hoy) {
            return String.format("%s-%d-%05d", prefijo(), hoy.getYear(), numero);
        }
    };

    private final String prefijo;

    SerieCodigo(String prefijo) {
        this.prefijo = prefijo;
    }

    public String prefijo() {
        return prefijo;
    }

    /**
     * Arma el código con el número que entregó la secuencia de la serie.
     *
     * @param hoy fecha actual en Colombia; solo la usa la reserva
     */
    public String formatear(long numero, LocalDate hoy) {
        return prefijo + "-" + numero;
    }
}
