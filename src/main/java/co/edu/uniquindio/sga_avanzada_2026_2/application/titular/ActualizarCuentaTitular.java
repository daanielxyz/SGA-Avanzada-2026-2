package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el huésped cambia su nombre y su teléfono desde su cuenta. El correo solo cambia con PIN
 * ({@link CambiarCorreoTitular}) y el documento nunca (TIT-01 · DEC-69).
 */
// TODO(B5): un titular validado por recepción ya no cambia su nombre desde la cuenta (DEC-68).
@Service
public class ActualizarCuentaTitular {

    private final TitularRepository titulares;

    public ActualizarCuentaTitular(TitularRepository titulares) {
        this.titulares = titulares;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el titular
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si falta el nombre o el
     *                                      teléfono es inválido (DEC-49)
     */
    @Transactional
    public TitularResult ejecutar(ActualizarCuentaTitularCommand comando) {
        TitularId id = new TitularId(comando.id());
        Titular titular = titulares.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el titular", id.valor()));

        titular.actualizarDatos(comando.nombre(), titular.correo(), comando.telefono());

        titulares.guardar(titular);
        return TitularResult.de(titular);
    }
}
