package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.MarcaAlojamiento;

/**
 * La marca de un alojamiento como datos simples, de entrada y de salida (DEC-31): todo opcional. Las reglas de formato
 * las aplica {@link MarcaAlojamiento}.
 */
public record MarcaDatos(ColoresDatos colores, FuentesDatos fuentes, RadiosDatos radio, String logoUrl,
                         String portadaUrl) {

    public record ColoresDatos(String fondo, String superficie, String texto, String textoSuave, String borde,
                               String bordeCampo, String acento, String textoSobreAcento, String peligro,
                               String exito) {
    }

    public record FuentesDatos(String titulos, String texto) {
    }

    public record RadiosDatos(String pequeno, String mediano, String grande) {
    }

    static MarcaDatos de(MarcaAlojamiento m) {
        MarcaAlojamiento.Colores c = m.colores();
        return new MarcaDatos(
                new ColoresDatos(c.fondo(), c.superficie(), c.texto(), c.textoSuave(), c.borde(), c.bordeCampo(),
                        c.acento(), c.textoSobreAcento(), c.peligro(), c.exito()),
                new FuentesDatos(m.fuentes().titulos(), m.fuentes().texto()),
                new RadiosDatos(m.radio().pequeno(), m.radio().mediano(), m.radio().grande()),
                m.logoUrl(), m.portadaUrl());
    }

    MarcaAlojamiento aDominio() {
        return new MarcaAlojamiento(
                new MarcaAlojamiento.Colores(colores.fondo(), colores.superficie(), colores.texto(),
                        colores.textoSuave(), colores.borde(), colores.bordeCampo(), colores.acento(),
                        colores.textoSobreAcento(), colores.peligro(), colores.exito()),
                new MarcaAlojamiento.Fuentes(fuentes.titulos(), fuentes.texto()),
                new MarcaAlojamiento.Radios(radio.pequeno(), radio.mediano(), radio.grande()),
                logoUrl, portadaUrl);
    }
}
