package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

/**
 * Por qué una respuesta salió del manual y no de la IA (DEC-127), con el aviso que lee la persona.
 */
public enum MotivoSinIa {
    /** El despliegue no tiene la IA encendida (modo local o sin clave). */
    IA_APAGADA("El asistente responde con el manual del PMS."),
    /** Quien pregunta llegó a su límite de preguntas por hora. */
    LIMITE_PERSONAL("Llegaste al límite de preguntas por hora: mientras tanto te respondo con el manual."),
    /** Se acabó la cuota del proveedor de IA para hoy. */
    CUOTA_AGOTADA("El asistente con IA llegó a su límite de hoy: te respondo con el manual."),
    /** El proveedor falló o no respondió a tiempo. */
    NO_DISPONIBLE("El asistente con IA no respondió: te respondo con el manual.");

    private final String aviso;

    MotivoSinIa(String aviso) {
        this.aviso = aviso;
    }

    public String aviso() {
        return aviso;
    }
}
