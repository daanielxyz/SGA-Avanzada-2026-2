package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

/**
 * Entrada de {@link CargarServicioAdicionalUseCase}: el servicio del catálogo del alojamiento que consumió el grupo.
 *
 * @param cantidad cuántas unidades, en un solo cargo (BL-12)
 */
public record CargarServicioAdicionalCommand(String codigoReserva, String servicioId, int cantidad) {

    /** Una sola unidad. */
    public CargarServicioAdicionalCommand(String codigoReserva, String servicioId) {
        this(codigoReserva, servicioId, 1);
    }
}
