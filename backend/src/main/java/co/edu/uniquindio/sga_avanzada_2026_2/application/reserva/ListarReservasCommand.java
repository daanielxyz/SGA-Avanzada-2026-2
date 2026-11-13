package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;
import java.util.List;

/**
 * Entrada de {@link ListarReservasUseCase} (CU-25). Todo es opcional salvo la página.
 *
 * @param estados nombres de {@code EstadoReserva}; nulo o vacío, cualquiera (#22 · DEC-119)
 * @param canales nombres de {@code CanalOrigen}; ídem
 * @param pagina empieza en 0 (DEC-18)
 */
public record ListarReservasCommand(LocalDate desde, LocalDate hasta, List<String> estados, List<String> apartamentos,
                                    List<String> canales, String titular, int pagina) {
}
