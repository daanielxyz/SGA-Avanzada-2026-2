package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

/** Cada dato del TRA que un ocupante debe tener antes de la salida (BL-07). El segundo apellido no es obligatorio. */
public enum DatoTra {
    DOCUMENTO("documento"),
    PRIMER_APELLIDO("primer apellido"),
    NACIONALIDAD("nacionalidad"),
    GENERO("género"),
    PAIS_RESIDENCIA("país de residencia"),
    CIUDAD_RESIDENCIA("ciudad de residencia"),
    CIUDAD_PROCEDENCIA("procedencia"),
    CIUDAD_DESTINO("destino"),
    MOTIVO_VIAJE("motivo del viaje");

    private final String texto;

    DatoTra(String texto) {
        this.texto = texto;
    }

    /** Cómo se nombra el dato en un mensaje para recepción. */
    public String texto() {
        return texto;
    }
}
