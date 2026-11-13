package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: recepción o el administrador actualizan el nombre y el contacto de un titular, también si su identidad
 * está validada, porque es el hotel (DEC-98). Su documento no cambia (TIT-05 · DEC-49).
 */
@Service
public class ActualizarTitularUseCase {

    private final TitularRepository titulares;
    private final DirectorioPersonal personal;

    public ActualizarTitularUseCase(TitularRepository titulares, DirectorioPersonal personal) {
        this.titulares = titulares;
        this.personal = personal;
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

        titular.actualizarDatos(comando.nombres(), comando.primerApellido(), comando.segundoApellido(),
                RegistrarTitularUseCase.correo(comando.correo()), comando.telefono());

        titulares.guardar(titular);
        return TitularResult.de(titular, personal);
    }
}
