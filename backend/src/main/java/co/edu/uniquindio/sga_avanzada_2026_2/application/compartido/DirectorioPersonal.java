package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.util.Optional;

/**
 * Puerto: el nombre de una persona del personal. El usuario vive en infraestructura y la reserva solo guarda su id
 * (BL-25 · DEC-103).
 */
public interface DirectorioPersonal {

    Optional<String> nombreDe(UsuarioId id);
}
