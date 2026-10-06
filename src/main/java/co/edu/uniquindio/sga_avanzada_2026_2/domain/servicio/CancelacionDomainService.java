package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Servicio de dominio sin estado: cancela una reserva y liquida la retención en su folio con la política congelada.
 * Cruza Reserva, Folio y PoliticaCancelacion, que recibe por parámetro; los dos primeros cambian en la misma
 * transacción (DEC-44).
 */
public class CancelacionDomainService {

    /**
     * Cancela la reserva y libera sus noches (RN-08 · RN-12, en {@code Reserva.cancelar}); la penalización sale del
     * tramo de la política congelada que corresponde a las horas que faltan para la entrada (RN-13 · RES-13 ·
     * POL-05), calculada sobre la base que eligió el alojamiento (DEC-41), y se liquida en el folio (DEC-43).
     *
     * @param politica   la versión congelada en la reserva ({@code PoliticaCancelacionRepository.buscarPorId})
     * @param parametros hora de entrada y porcentaje de anticipo del alojamiento
     * @param ahora      fecha y hora actuales en Colombia, inyectadas
     * @return lo retenido; cero si el tramo no penaliza
     * @throws ReglaDominioException si la política no es la congelada en la reserva (RN-13), el folio es de otra
     *                               reserva (FOL-02) o la reserva no puede cancelarse (RN-08)
     */
    public Dinero procesarCancelacion(Reserva reserva, Folio folio, PoliticaCancelacion politica,
                                      ParametrosAlojamiento parametros, LocalDateTime ahora) {
        validarCorrespondencia(reserva, folio, politica);
        long horasAntelacion = Duration.between(ahora,
                reserva.estancia().entrada().atTime(parametros.horaEntrada())).toHours();
        Penalizacion penalizacion = politica.penalizacionPara(horasAntelacion);
        reserva.cancelar();
        return liquidar(reserva, folio, penalizacion, parametros, "cancelación", ahora);
    }

    // RN-13 · FOL-02: la retención sale de la política que el huésped aceptó, nunca de la vigente
    static void validarCorrespondencia(Reserva reserva, Folio folio, PoliticaCancelacion politica) {
        if (!politica.id().equals(reserva.politicaVersionId())) {
            throw new ReglaDominioException("La reserva " + reserva.codigo().valor() + " congeló la política "
                    + reserva.politicaVersionId().valor() + ", no la " + politica.id().valor());
        }
        if (!folio.reservaId().equals(reserva.codigo())) {
            throw new ReglaDominioException("El folio " + folio.id().valor() + " no es de la reserva "
                    + reserva.codigo().valor());
        }
    }

    // DEC-41 · DEC-43
    static Dinero liquidar(Reserva reserva, Folio folio, Penalizacion penalizacion, ParametrosAlojamiento parametros,
                           String motivo, LocalDateTime ahora) {
        Dinero valorTotal = reserva.valorTotal();
        Dinero retenido = penalizacion.calcular(valorTotal, folio.totalPagado(),
                valorTotal.porcentaje(parametros.anticipo()));
        folio.liquidarPenalidad(valorTotal, retenido, motivo, ahora.toLocalDate());
        return retenido;
    }
}
