package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

/**
 * Tipos de documento de identidad admitidos.
 */
public enum TipoDocumento {
    CC,
    CE,
    PASAPORTE,
    TI,
    /**
     * Registro civil: el de un niño menor de 7 años, que todavía no tiene tarjeta de identidad. Solo identifica a un
     * ocupante, nunca al titular ni a un usuario (DEC de las pruebas del sprint 3).
     */
    RC;

    /** El titular responde por la reserva y su folio y entra a su cuenta con el documento: no lo hace con RC. */
    public boolean identificaTitular() {
        return this != RC;
    }
}
