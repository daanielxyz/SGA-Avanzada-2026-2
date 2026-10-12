package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.CargoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.LocalDate;

/**
 * Servicio de dominio sin estado: cobra en el folio un servicio adicional del catálogo. Cruza Reserva, Folio y
 * Alojamiento, que recibe por parámetro; solo cambia el Folio (DEC-60).
 */
public class CargoServicioDomainService {

    /**
     * Agrega al folio el cargo del servicio con su valor de este momento, que queda congelado (CAR-05 · SERV-02).
     * Solo se cobra mientras el grupo está alojado (DEC-60).
     *
     * @param hoy fecha actual en Colombia, inyectada; queda como fecha del cargo
     * @return el id del cargo nuevo
     * @throws ReglaDominioException si el folio es de otra reserva (FOL-02), la reserva no está EN_CURSO, el
     *                               servicio no existe, está inactivo (SERV-04), no genera cargo o su valor es cero
     *                               (CAR-06), o el folio está cerrado (CAR-07)
     */
    public CargoId cargar(Reserva reserva, Folio folio, Alojamiento alojamiento, ServicioAdicionalId servicioId,
                          LocalDate hoy) {
        if (!folio.reservaId().equals(reserva.codigo())) {
            throw new ReglaDominioException("El folio " + folio.id().valor() + " no es de la reserva "
                    + reserva.codigo().valor());
        }
        if (reserva.estado() != EstadoReserva.EN_CURSO) {
            throw new ReglaDominioException("Solo se cobran servicios a una reserva EN_CURSO; está "
                    + reserva.estado());
        }
        ServicioAdicional servicio = alojamiento.servicioCobrable(servicioId);
        return folio.agregarServicioAdicional(servicio.nombre(), servicio.valor(), hoy);
    }
}
