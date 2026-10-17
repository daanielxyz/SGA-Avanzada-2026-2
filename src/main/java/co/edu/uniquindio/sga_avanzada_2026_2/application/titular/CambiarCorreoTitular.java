package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: cambiar el correo del titular, que es también el de su cuenta. Lo llama la seguridad después de
 * verificar el PIN del cambio de correo (DEC-68 · DEC-69).
 */
@Service
public class CambiarCorreoTitular {

    private final TitularRepository titulares;

    public CambiarCorreoTitular(TitularRepository titulares) {
        this.titulares = titulares;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el titular
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el correo es inválido
     */
    @Transactional
    public TitularResult ejecutar(CambiarCorreoTitularCommand comando) {
        TitularId id = new TitularId(comando.id());
        Titular titular = titulares.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el titular", id.valor()));

        titular.cambiarCorreo(new Correo(comando.nuevoCorreo()));

        titulares.guardar(titular);
        return TitularResult.de(titular);
    }
}
