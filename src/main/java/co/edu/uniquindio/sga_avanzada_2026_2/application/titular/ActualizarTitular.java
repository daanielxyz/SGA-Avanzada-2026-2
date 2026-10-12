package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: actualizar el nombre y el contacto de un titular. Su documento no cambia (TIT-01 · DEC-49).
 */
@Service
public class ActualizarTitular {

    private final TitularRepository titulares;

    public ActualizarTitular(TitularRepository titulares) {
        this.titulares = titulares;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el titular
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si falta el nombre, no
     *                                      queda ningún contacto o alguno es inválido (DEC-49)
     */
    @Transactional
    public TitularResult ejecutar(ActualizarTitularCommand comando) {
        TitularId id = new TitularId(comando.id());
        Titular titular = titulares.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el titular", id.valor()));

        titular.actualizarDatos(comando.nombre(), RegistrarTitular.correo(comando.correo()), comando.telefono());

        titulares.guardar(titular);
        return TitularResult.de(titular);
    }
}
