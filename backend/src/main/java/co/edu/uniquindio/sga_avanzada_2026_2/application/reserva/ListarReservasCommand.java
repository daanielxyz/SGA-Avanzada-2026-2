package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;

/**
 * Entrada de {@link ListarReservasUseCase} (CU-25). Todo es opcional salvo la página.
 *
 * @param estado nombre de {@code EstadoReserva}
 * @param canal  nombre de {@code CanalOrigen}
 * @param pagina empieza en 0 (DEC-18)
 */
public record ListarReservasCommand(LocalDate desde, LocalDate hasta, String estado, String apartamento,
                                    String canal, String titular, int pagina) {
}
