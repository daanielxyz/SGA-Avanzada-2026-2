package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.CambiarMarcaAlojamientoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.MarcaDatos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/alojamiento/marca} (BL-05): reemplaza toda la marca. Lo que no se envía queda vacío y la web
 * usa su valor neutro.
 */
public record MarcaRequest(
        @NotNull(message = "es obligatorio")
        @Valid
        ColoresRequest colores,

        @NotNull(message = "es obligatorio")
        @Valid
        FuentesRequest fuentes,

        @NotNull(message = "es obligatorio")
        @Valid
        RadiosRequest radio,

        @Pattern(regexp = IMAGEN, message = MENSAJE_IMAGEN)
        @Size(max = 2000, message = "admite máximo 2000 caracteres")
        String logoUrl,

        @Pattern(regexp = IMAGEN, message = MENSAJE_IMAGEN)
        @Size(max = 2000, message = "admite máximo 2000 caracteres")
        String portadaUrl) {

    private static final String COLOR = "#[0-9A-Fa-f]{6}";
    private static final String MENSAJE_COLOR = "debe ser un color #RRGGBB";
    private static final String FUENTE = "[\\p{L}0-9 ]{1,60}";
    private static final String MENSAJE_FUENTE = "solo admite letras, números y espacios, máximo 60";
    private static final String RADIO = "\\d{1,2}px";
    private static final String MENSAJE_RADIO = "debe ser un radio en píxeles, por ejemplo 12px";
    private static final String IMAGEN = "(https?://|data:image/)\\S+";
    private static final String MENSAJE_IMAGEN = "debe ser una dirección http(s) o una imagen data:image";

    public record ColoresRequest(
            @Pattern(regexp = COLOR, message = MENSAJE_COLOR) String fondo,
            @Pattern(regexp = COLOR, message = MENSAJE_COLOR) String superficie,
            @Pattern(regexp = COLOR, message = MENSAJE_COLOR) String texto,
            @Pattern(regexp = COLOR, message = MENSAJE_COLOR) String textoSuave,
            @Pattern(regexp = COLOR, message = MENSAJE_COLOR) String borde,
            @Pattern(regexp = COLOR, message = MENSAJE_COLOR) String bordeCampo,
            @Pattern(regexp = COLOR, message = MENSAJE_COLOR) String acento,
            @Pattern(regexp = COLOR, message = MENSAJE_COLOR) String textoSobreAcento,
            @Pattern(regexp = COLOR, message = MENSAJE_COLOR) String peligro,
            @Pattern(regexp = COLOR, message = MENSAJE_COLOR) String exito) {
    }

    public record FuentesRequest(
            @Pattern(regexp = FUENTE, message = MENSAJE_FUENTE) String titulos,
            @Pattern(regexp = FUENTE, message = MENSAJE_FUENTE) String texto) {
    }

    public record RadiosRequest(
            @Pattern(regexp = RADIO, message = MENSAJE_RADIO) String pequeno,
            @Pattern(regexp = RADIO, message = MENSAJE_RADIO) String mediano,
            @Pattern(regexp = RADIO, message = MENSAJE_RADIO) String grande) {
    }

    CambiarMarcaAlojamientoCommand aCommand(String alojamientoId) {
        return new CambiarMarcaAlojamientoCommand(alojamientoId, new MarcaDatos(
                new MarcaDatos.ColoresDatos(colores.fondo(), colores.superficie(), colores.texto(),
                        colores.textoSuave(), colores.borde(), colores.bordeCampo(), colores.acento(),
                        colores.textoSobreAcento(), colores.peligro(), colores.exito()),
                new MarcaDatos.FuentesDatos(fuentes.titulos(), fuentes.texto()),
                new MarcaDatos.RadiosDatos(radio.pequeno(), radio.mediano(), radio.grande()),
                logoUrl, portadaUrl));
    }
}
