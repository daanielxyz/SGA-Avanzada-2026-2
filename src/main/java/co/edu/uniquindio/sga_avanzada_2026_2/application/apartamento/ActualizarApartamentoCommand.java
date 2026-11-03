package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import java.util.List;

/**
 * Entrada de {@link ActualizarApartamentoUseCase} (CU-31): los datos descriptivos, no la capacidad (APA-15).
 */
public record ActualizarApartamentoCommand(String codigo, String nombre, String descripcion, int dormitorios,
                                           List<String> caracteristicas) {
}
