package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import java.util.Optional;

/**
 * Puerto de persistencia del agregado Alojamiento. Guarda y carga el agregado completo (raíz + parámetros,
 * servicios adicionales y medios de pago).
 */
public interface AlojamientoRepository {

    void guardar(Alojamiento alojamiento);

    Optional<Alojamiento> buscarPorId(AlojamientoId id);
}
