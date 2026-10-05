package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import java.util.List;

/**
 * Entrada del caso de uso {@link CrearApartamento} (DEC-31). Datos simples, sin HTTP: los VO del dominio se crean
 * dentro del caso de uso y validan las reglas.
 */
public record CrearApartamentoCommand(String codigo, String alojamientoId, String nombre, String descripcion,
                                      int capacidad, int dormitorios, List<ImagenCommand> imagenes,
                                      List<String> caracteristicas) {

    public record ImagenCommand(String url, boolean principal) {
    }
}
