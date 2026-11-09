package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el huésped cambia su nombre y su teléfono desde su cuenta; con la identidad validada por recepción, el
 * nombre ya no (DEC-98). El correo solo cambia con PIN ({@link CambiarCorreoTitularUseCase}) y el documento nunca
 * (TIT-05 · DEC-69).
 */
@Service
public class ActualizarCuentaTitularUseCase {

    private final TitularRepository titulares;

    public ActualizarCuentaTitularUseCase(TitularRepository titulares) {
        this.titulares = titulares;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el titular
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la identidad está
     *                                      validada y el nombre cambia (DEC-98), falta el nombre o el teléfono es
     *                                      inválido (DEC-49)
     */
    @Transactional
    public TitularResult ejecutar(ActualizarCuentaTitularCommand comando) {
        TitularId id = new TitularId(comando.id());
        Titular titular = titulares.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el titular", id.valor()));

        titular.actualizarDesdeCuenta(comando.nombres(), comando.primerApellido(), comando.segundoApellido(),
                comando.telefono());

        titulares.guardar(titular);
        return TitularResult.de(titular);
    }
}
