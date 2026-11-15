package co.edu.uniquindio.sga_avanzada_2026_2.domain.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;

import java.time.LocalDate;
import java.util.List;

/**
 * VO: la Tarjeta de Registro de Alojamiento de una estancia, lista para enviar (Resolución 409 de 2022, DEC-130). El
 * huésped principal lleva además el motivo, el número de acompañantes, el costo y los datos del establecimiento.
 *
 * @param numeroHabitacion el código del apartamento
 * @param costo            el valor total de la reserva
 */
public record ReporteTra(ReservaId reserva, HuespedTra principal, List<HuespedTra> acompanantes, String motivo,
                         LocalDate entrada, LocalDate salida, String numeroHabitacion, String tipoAcomodacion,
                         Dinero costo, String nombreEstablecimiento, String rnt) {

    // DEC-130
    public ReporteTra {
        if (reserva == null || principal == null || acompanantes == null || entrada == null || salida == null
                || costo == null || vacio(motivo) || vacio(numeroHabitacion) || vacio(tipoAcomodacion)
                || vacio(nombreEstablecimiento) || vacio(rnt)) {
            throw new ReglaDominioException("Faltan datos del reporte del TRA");
        }
        acompanantes = List.copyOf(acompanantes);
    }

    public int numeroAcompanantes() {
        return acompanantes.size();
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
