package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.MarcaDatos;

/**
 * JSON de la marca del alojamiento (BL-05), que la web aplica sobre su esqueleto (DEC-83). Un valor vacío significa que
 * la web usa el suyo neutro.
 */
public record MarcaResponse(ColoresResponse colores, FuentesResponse fuentes, RadiosResponse radio, String logoUrl,
                            String portadaUrl) {

    public record ColoresResponse(String fondo, String superficie, String texto, String textoSuave, String borde,
                                  String bordeCampo, String acento, String textoSobreAcento, String peligro,
                                  String exito) {
    }

    public record FuentesResponse(String titulos, String texto) {
    }

    public record RadiosResponse(String pequeno, String mediano, String grande) {
    }

    static MarcaResponse de(MarcaDatos m) {
        MarcaDatos.ColoresDatos c = m.colores();
        return new MarcaResponse(
                new ColoresResponse(c.fondo(), c.superficie(), c.texto(), c.textoSuave(), c.borde(), c.bordeCampo(),
                        c.acento(), c.textoSobreAcento(), c.peligro(), c.exito()),
                new FuentesResponse(m.fuentes().titulos(), m.fuentes().texto()),
                new RadiosResponse(m.radio().pequeno(), m.radio().mediano(), m.radio().grande()),
                m.logoUrl(), m.portadaUrl());
    }
}
