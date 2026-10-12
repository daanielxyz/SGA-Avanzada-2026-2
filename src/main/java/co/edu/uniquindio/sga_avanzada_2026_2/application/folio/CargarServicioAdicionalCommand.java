package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

/**
 * Entrada de {@link CargarServicioAdicional}: el servicio del catálogo del alojamiento que consumió el grupo.
 */
public record CargarServicioAdicionalCommand(String codigoReserva, String servicioId) {
}
