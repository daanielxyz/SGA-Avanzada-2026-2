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
        Penalizacion penalizacion = penalizacionAl(ahora, reserva, politica, parametros);
        reserva.cancelar();
        return liquidar(reserva, folio, penalizacion, parametros, "cancelación", ahora);
    }

    /**
     * Calcula lo que dejaría {@link #procesarCancelacion} en este momento, sin cambiar nada, para que el huésped lo
     * sepa antes de decidir (DEC-91). Puede cambiar más tarde, al pasar a otro tramo de la política.
     *
     * @param politica   la versión congelada en la reserva
     * @param parametros hora de entrada y porcentaje de anticipo del alojamiento
     * @param ahora      fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si la política no es la congelada en la reserva (RN-13), el folio es de otra
     *                               reserva (FOL-02) o está cerrado, o la reserva no puede cancelarse (RN-08)
     */
    public SimulacionCancelacion simularCancelacion(Reserva reserva, Folio folio, PoliticaCancelacion politica,
                                                    ParametrosAlojamiento parametros, LocalDateTime ahora) {
        validarCorrespondencia(reserva, folio, politica);
        reserva.exigirCancelable();
        Dinero retenido = retencion(reserva, folio, penalizacionAl(ahora, reserva, politica, parametros), parametros);
        return new SimulacionCancelacion(retenido, folio.saldoSiSeLiquida(reserva.valorConExtras(), retenido));
    }

    // RN-13 · POL-05: el tramo sale de las horas que faltan para la entrada
    private static Penalizacion penalizacionAl(LocalDateTime ahora, Reserva reserva, PoliticaCancelacion politica,
                                               ParametrosAlojamiento parametros) {
        return politica.penalizacionPara(Duration.between(ahora,
                reserva.estancia().entrada().atTime(parametros.horaEntrada())).toHours());
    }

    /**
     * Cancela automáticamente una reserva PENDIENTE que superó el plazo de confirmación (RN-21 · RES-21, en
     * {@code Reserva.expirar}) y anula su alojamiento en el folio sin penalidad: lo pagado queda a favor para la
     * devolución (DEC-57).
     *
     * @param parametros plazos del alojamiento: el del pago en línea para el portal y el de confirmación para los
     *                   demás
     * @param ahora      fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si el folio es de otra reserva (FOL-02), la reserva no está PENDIENTE o todavía
     *                               no supera el plazo
     */
    public void procesarVencimiento(Reserva reserva, Folio folio, ParametrosAlojamiento parametros,
                                    LocalDateTime ahora) {
        validarFolio(reserva, folio);
        reserva.expirar(ahora, parametros.plazoParaConfirmar(reserva.canalOrigen()));
        folio.liquidarPenalidad(reserva.valorTotal(), Dinero.CERO, "vencimiento", ahora.toLocalDate());
        folio.anularExtras(reserva.extras(), "vencimiento", ahora.toLocalDate());
    }

    /**
     * Libera una reserva PENDIENTE sin pagos porque quien la hizo volvió desde el pago (DEC-93): como el vencimiento
     * (RN-21), anula su alojamiento y sus extras en el folio sin penalidad, pero sin esperar el plazo. Con algún pago
     * ya no es arrepentirse antes de pagar: se cancela con la política (RN-13).
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si el folio es de otra reserva (FOL-02), ya tiene pagos o la reserva no está
     *                               PENDIENTE
     */
    public void procesarLiberacion(Reserva reserva, Folio folio, LocalDateTime ahora) {
        validarFolio(reserva, folio);
        if (folio.totalPagado().esPositivo()) {
            throw new ReglaDominioException("La reserva " + reserva.codigo().valor()
                    + " ya tiene pagos: se cancela con su política de cancelación");
        }
        reserva.liberar();
        folio.liquidarPenalidad(reserva.valorTotal(), Dinero.CERO, "liberación", ahora.toLocalDate());
        folio.anularExtras(reserva.extras(), "liberación", ahora.toLocalDate());
    }

    // RN-13 · FOL-02: la retención sale de la política que el huésped aceptó, nunca de la vigente
    static void validarCorrespondencia(Reserva reserva, Folio folio, PoliticaCancelacion politica) {
        if (!politica.id().equals(reserva.politicaVersionId())) {
            throw new ReglaDominioException("La reserva " + reserva.codigo().valor() + " congeló la política "
                    + reserva.politicaVersionId().valor() + ", no la " + politica.id().valor());
        }
        validarFolio(reserva, folio);
    }

    // FOL-02
    private static void validarFolio(Reserva reserva, Folio folio) {
        if (!folio.reservaId().equals(reserva.codigo())) {
            throw new ReglaDominioException("El folio " + folio.id().valor() + " no es de la reserva "
                    + reserva.codigo().valor());
        }
    }

    // DEC-41 · DEC-43 · DEC-89: se retiene solo sobre la estancia; los extras se anulan y lo pagado por ellos se devuelve
    static Dinero liquidar(Reserva reserva, Folio folio, Penalizacion penalizacion, ParametrosAlojamiento parametros,
                           String motivo, LocalDateTime ahora) {
        Dinero retenido = retencion(reserva, folio, penalizacion, parametros);
        folio.liquidarPenalidad(reserva.valorTotal(), retenido, motivo, ahora.toLocalDate());
        folio.anularExtras(reserva.extras(), motivo, ahora.toLocalDate());
        return retenido;
    }

    private static Dinero retencion(Reserva reserva, Folio folio, Penalizacion penalizacion,
                                    ParametrosAlojamiento parametros) {
        Dinero valorTotal = reserva.valorTotal();
        return penalizacion.calcular(valorTotal, reserva.pagadoPorLaEstancia(folio.totalPagado()),
                valorTotal.porcentaje(parametros.anticipo()));
    }
}
