package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.LocalDateTime;

/**
 * Servicio de dominio sin estado: declara el no-show y liquida su consecuencia en el folio con la política congelada.
 * Cruza Reserva, Folio y PoliticaCancelacion, que recibe por parámetro (DEC-44).
 */
public class NoShowDomainService {

    /**
     * Declara el no-show y libera las noches (RN-08 · RN-12; la hora límite la valida
     * {@code Reserva.declararNoShow}, RES-16) y liquida en el folio la penalización de no-show de la política
     * congelada (RN-13 · POL-06), calculada sobre la base que eligió el alojamiento (DEC-41).
     *
     * @param politica   la versión congelada en la reserva
     * @param parametros hora límite de no-show y porcentaje de anticipo del alojamiento
     * @param ahora      fecha y hora actuales en Colombia, inyectadas
     * @return lo retenido
     * @throws ReglaDominioException si la política no es la congelada (RN-13), el folio es de otra reserva (FOL-02),
     *                               la reserva no está CONFIRMADA (RN-08) o aún no llega la hora límite (RES-16)
     */
    public Dinero declararNoShow(Reserva reserva, Folio folio, PoliticaCancelacion politica,
                                 ParametrosAlojamiento parametros, LocalDateTime ahora) {
        CancelacionDomainService.validarCorrespondencia(reserva, folio, politica);
        reserva.declararNoShow(ahora, parametros.horaLimiteNoShow());
        return CancelacionDomainService.liquidar(reserva, folio, politica.penalizacionNoShow(), parametros,
                "no-show", ahora);
    }
}
