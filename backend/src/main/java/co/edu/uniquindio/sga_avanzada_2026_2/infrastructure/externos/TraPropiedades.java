package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * La configuración del envío del TRA, de {@code sga.tra.*} (DEC-130). El token llega solo por variable de entorno
 * ({@code SGA_TRA_TOKEN}) y nunca se escribe en el log.
 *
 * @param modo                 {@code local} (por defecto: no sale a internet) o {@code mincit}
 * @param token                el que el Ministerio le manda al correo del RNT del hotel
 * @param url                  la base de la API para PMS, p. ej. {@code https://pms.mincit.gov.co}
 * @param tiempoLimiteSegundos lo que se espera al Ministerio antes de dejar el envío con error
 */
@ConfigurationProperties("sga.tra")
public record TraPropiedades(String modo, String token, String url, int tiempoLimiteSegundos) {

    public boolean tieneToken() {
        return token != null && !token.isBlank();
    }

    @Override
    public String toString() {
        return "TraPropiedades[modo=" + modo + ", url=" + url + ", token=" + (tieneToken() ? "***" : "") + "]";
    }
}
