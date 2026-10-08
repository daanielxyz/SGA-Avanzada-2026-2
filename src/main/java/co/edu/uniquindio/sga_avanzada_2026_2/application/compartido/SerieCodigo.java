package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

/**
 * Series de códigos de negocio que el sistema genera (DEC-52): {@code PREFIJO-n}. Los movimientos del folio no
 * están aquí porque los numera el propio folio (DEC-43).
 */
public enum SerieCodigo {
    SERVICIO("SRV"),
    POLITICA("POL"),
    TEMPORADA("TEM"),
    TARIFA("TAR"),
    BLOQUEO("BLO");

    private final String prefijo;

    SerieCodigo(String prefijo) {
        this.prefijo = prefijo;
    }

    public String prefijo() {
        return prefijo;
    }
}
