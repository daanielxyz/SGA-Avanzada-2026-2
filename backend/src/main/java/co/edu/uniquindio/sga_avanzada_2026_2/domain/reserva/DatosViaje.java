package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

/**
 * Datos del viaje de un ocupante que pide el TRA y que recepción toma en la llegada (BL-07). Cada uno puede faltar
 * mientras la estancia no termina: la llegada se registra con datos incompletos y la salida los exige.
 */
public record DatosViaje(Genero genero, String paisResidencia, String ciudadResidencia, String ciudadProcedencia,
                         String ciudadDestino, MotivoViaje motivoViaje) {

    /** Un ocupante recién creado: todavía no tiene ningún dato del viaje. */
    public static final DatosViaje VACIOS = new DatosViaje(null, null, null, null, null, null);

    public DatosViaje {
        paisResidencia = limpio(paisResidencia);
        ciudadResidencia = limpio(ciudadResidencia);
        ciudadProcedencia = limpio(ciudadProcedencia);
        ciudadDestino = limpio(ciudadDestino);
    }

    /** Completa con los datos que llegan; el que no llega se conserva (BL-07). */
    public DatosViaje completarCon(DatosViaje nuevos) {
        if (nuevos == null) {
            return this;
        }
        return new DatosViaje(
                nuevos.genero != null ? nuevos.genero : genero,
                nuevos.paisResidencia != null ? nuevos.paisResidencia : paisResidencia,
                nuevos.ciudadResidencia != null ? nuevos.ciudadResidencia : ciudadResidencia,
                nuevos.ciudadProcedencia != null ? nuevos.ciudadProcedencia : ciudadProcedencia,
                nuevos.ciudadDestino != null ? nuevos.ciudadDestino : ciudadDestino,
                nuevos.motivoViaje != null ? nuevos.motivoViaje : motivoViaje);
    }

    static String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
