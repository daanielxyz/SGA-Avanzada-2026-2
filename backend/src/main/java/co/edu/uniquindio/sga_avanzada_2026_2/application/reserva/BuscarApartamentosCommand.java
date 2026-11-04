package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.FiltroBusqueda;

import java.time.LocalDate;
import java.util.List;

/**
 * Entrada de {@link BuscarApartamentosUseCase} (CU-07): fechas, composición del grupo y filtros.
 *
 * @param fechasNacimiento opcional: con ellas el valor estimado cuenta solo a los facturables (RN-06); sin ellas,
 *                         todos pagan
 * @param pagina           empieza en 0
 */
public record BuscarApartamentosCommand(String alojamientoId, LocalDate entrada, LocalDate salida, int ocupantes,
                                        List<LocalDate> fechasNacimiento, FiltroBusqueda filtro, int pagina) {
}
