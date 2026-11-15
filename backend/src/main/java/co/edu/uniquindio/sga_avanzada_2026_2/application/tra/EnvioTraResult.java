package co.edu.uniquindio.sga_avanzada_2026_2.application.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EnvioTra;

import java.time.LocalDateTime;

/**
 * Salida de los casos de uso del envío del TRA (DEC-31 · DEC-130): en qué va, sin los datos personales enviados.
 *
 * @param estado               PENDIENTE, ENVIADO o CON_ERROR
 * @param codigoMinisterio     el que devolvió el Ministerio para el huésped principal; null si aún no entró
 * @param acompanantesEnviados cuántos acompañantes ya entraron
 */
public record EnvioTraResult(String reserva, String estado, String codigoMinisterio, int acompanantesEnviados,
                             int intentos, LocalDateTime ultimoIntento, String ultimoError, LocalDateTime enviadoEn) {

    public static EnvioTraResult de(EnvioTra envio) {
        return new EnvioTraResult(envio.reserva().valor(), envio.estado().name(), envio.codigoPrincipal(),
                envio.acompanantesEnviados().size(), envio.intentos(), envio.ultimoIntento(), envio.ultimoError(),
                envio.enviadoEn());
    }
}
