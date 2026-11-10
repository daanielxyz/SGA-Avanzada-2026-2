package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adaptador del directorio del personal sobre los usuarios internos (BL-25 · DEC-103). */
@Component
class DirectorioPersonalJpa implements DirectorioPersonal {

    private final UsuarioRepository usuarios;

    DirectorioPersonalJpa(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<String> nombreDe(UsuarioId id) {
        return usuarios.findById(id.valor()).map(Usuario::nombre);
    }
}
