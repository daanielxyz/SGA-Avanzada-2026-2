package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;

import java.time.LocalDate;

/**
 * VO: los filtros con los que recepción busca reservas (CU-25 · DEC-72). Todos son opcionales; nulo es «cualquiera».
 *
 * @param desde con {@code hasta}, el rango [desde, hasta): entra toda reserva cuya estancia se cruce con él, con la
 *              misma regla de solapamiento de las estancias (EST-03)
 */
public record FiltroReservas(LocalDate desde, LocalDate hasta, EstadoReserva estado, ApartamentoId apartamento,
                             CanalOrigen canal, TitularId titular) {

    // EST-01: un rango vacío o al revés no tiene noches
    public FiltroReservas {
        if (desde != null && hasta != null && !desde.isBefore(hasta)) {
            throw new ReglaDominioException("La fecha desde debe ser anterior a la fecha hasta");
        }
    }
}
