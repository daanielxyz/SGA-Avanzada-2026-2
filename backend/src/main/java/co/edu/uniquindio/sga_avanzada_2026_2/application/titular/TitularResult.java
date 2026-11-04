package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;

import java.util.Optional;

/**
 * Salida de los casos de uso del Titular (DEC-31).
 */
public record TitularResult(String id, String alojamientoId, String nombre, String tipoDocumento,
                            String numeroDocumento, String correo, String telefono) {

    public static TitularResult de(Titular titular) {
        return new TitularResult(titular.id().valor(), titular.alojamientoId().valor(), titular.nombre(),
                titular.documento().tipo().name(), titular.documento().numero(),
                Optional.ofNullable(titular.correo()).map(Correo::valor).orElse(null), titular.telefono());
    }
}
