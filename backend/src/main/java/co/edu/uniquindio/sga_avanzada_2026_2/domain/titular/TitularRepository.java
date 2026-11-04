package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;

import java.util.List;
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

    /**
     * Titulares del alojamiento con ese correo, para el inicio de sesión del huésped, que lo distingue por su
     * documento: dos titulares, p. ej. de la misma familia, pueden compartir correo (DEC-69).
     */
    List<Titular> buscarPorCorreo(AlojamientoId alojamientoId, Correo correo);
}
