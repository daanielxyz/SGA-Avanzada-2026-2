package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * La configuración del asistente de IA, de {@code sga.asistente.*} (DEC-127). La clave llega solo por variable de
 * entorno ({@code SGA_IA_CLAVE}) y nunca se escribe en el log.
 *
 * @param modo                  {@code local} o {@code gemini}
 * @param url                   la base de la API de Gemini, p. ej. {@code https://generativelanguage.googleapis.com/v1beta}
 * @param tiempoLimiteSegundos  lo que se espera a Gemini antes de responder con el manual
 * @param maximoTokensRespuesta el largo máximo de la respuesta del modelo
 * @param preguntasPorHora      el cupo de cada persona, para cuidar la cuota gratis del proyecto
 */
@ConfigurationProperties("sga.asistente")
public record AsistentePropiedades(String modo, String clave, String modelo, String url, int tiempoLimiteSegundos,
                                   int maximoTokensRespuesta, int preguntasPorHora) {

    public boolean tieneClave() {
        return clave != null && !clave.isBlank();
    }

    @Override
    public String toString() {
        return "AsistentePropiedades[modo=" + modo + ", modelo=" + modelo + ", clave=" + (tieneClave() ? "***" : "")
                + "]";
    }
}
