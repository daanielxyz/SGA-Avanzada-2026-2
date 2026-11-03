package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;
import java.util.List;

/**
 * Una fila del calendario de ocupación (CU-24): un apartamento, sus reservas activas y sus bloqueos en el rango.
 */
public record OcupacionResult(String apartamento, String nombre, String estadoOperativo, List<ReservaOcupada> reservas,
                              List<BloqueoOcupado> bloqueos) {

    public record ReservaOcupada(String codigo, LocalDate entrada, LocalDate salida, String estado, String canalOrigen,
                                 String titularId) {
    }

    public record BloqueoOcupado(String id, LocalDate fechaInicio, LocalDate fechaFin, String motivo) {
    }
}
