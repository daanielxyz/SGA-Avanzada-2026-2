package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Un apartamento libre para la estancia buscada (CU-07). El precio se pide después con {@link CotizarEstancia}.
 */
public record ApartamentoDisponibleResult(String codigo, String nombre, int capacidad, int dormitorios) {
}
