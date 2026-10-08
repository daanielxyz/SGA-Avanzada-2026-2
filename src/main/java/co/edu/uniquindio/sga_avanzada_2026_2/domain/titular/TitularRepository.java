package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;

import java.util.Optional;

/**
 * Puerto de persistencia del agregado Titular.
 */
public interface TitularRepository {

    void guardar(Titular titular);

    Optional<Titular> buscarPorId(TitularId id);

    /**
     * Un mismo titular conserva su identidad entre reservas del alojamiento (TIT-05): se busca por documento antes de
     * crearlo. Cada alojamiento del SaaS tiene sus propios titulares (DEC-26).
     */
    Optional<Titular> buscarPorDocumento(AlojamientoId alojamientoId, Documento documento);
}
