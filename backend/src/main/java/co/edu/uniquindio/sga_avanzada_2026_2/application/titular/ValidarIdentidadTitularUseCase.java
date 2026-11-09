package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: recepción valida en persona, con el documento, la identidad de un titular (DEC-98). Desde entonces
 * su nombre solo lo cambia el hotel.
 */
@Service
public class ValidarIdentidadTitularUseCase {

    private final TitularRepository titulares;
    private final Clock reloj;

    public ValidarIdentidadTitularUseCase(TitularRepository titulares, Clock reloj) {
        this.titulares = titulares;
        this.reloj = reloj;
    }

    /**
     * @param autor id del usuario de la sesión
     * @throws RecursoNoEncontradoException si no existe el titular
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si ya estaba validada
     */
    @Transactional
    public TitularResult ejecutar(String titularId, String autor) {
        TitularId id = new TitularId(titularId);
        Titular titular = titulares.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el titular", id.valor()));

        titular.validarIdentidad(new UsuarioId(autor), LocalDateTime.now(reloj));

        titulares.guardar(titular);
        return TitularResult.de(titular);
    }
}
