package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tra.EnvioTraResult;

import java.time.LocalDateTime;

/**
 * JSON del envío del TRA de una reserva (DEC-31 · DEC-130), sin los datos personales enviados.
 *
 * @param estado               PENDIENTE, ENVIADO o CON_ERROR
 * @param codigoMinisterio     el que devolvió el Ministerio para el huésped principal; null si aún no entró
 * @param acompanantesEnviados cuántos acompañantes ya entraron
 * @param ultimoError          por qué falló el último intento; solo CON_ERROR
 */
public record EnvioTraResponse(String reserva, String estado, String codigoMinisterio, int acompanantesEnviados,
                               int intentos, LocalDateTime ultimoIntento, String ultimoError,
                               LocalDateTime enviadoEn) {

    static EnvioTraResponse de(EnvioTraResult r) {
        return new EnvioTraResponse(r.reserva(), r.estado(), r.codigoMinisterio(), r.acompanantesEnviados(),
                r.intentos(), r.ultimoIntento(), r.ultimoError(), r.enviadoEn());
    }
}
