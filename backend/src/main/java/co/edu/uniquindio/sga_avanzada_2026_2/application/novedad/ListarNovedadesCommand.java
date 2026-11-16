package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

import java.util.List;

/**
 * Entrada de {@link ListarNovedadesUseCase} (DEC-132): cada filtro con varios valores o vacío («cualquiera»). Los
 * estados y las gravedades llegan ya revisados en el borde.
 *
 * @param numeroPagina empieza en 0
 */
public record ListarNovedadesCommand(List<String> estados, List<String> gravedades, List<String> apartamentos,
                                     int numeroPagina) {
}
