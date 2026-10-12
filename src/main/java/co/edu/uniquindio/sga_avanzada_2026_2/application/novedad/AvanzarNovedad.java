package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.Novedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: dar el siguiente paso de una novedad, ABIERTA → EN_REVISION → CERRADA, dejando autor y fecha (CU-55 ·
 * NOV-04).
 */
@Service
public class AvanzarNovedad {

    private final NovedadRepository novedades;
    private final Clock reloj;

    public AvanzarNovedad(NovedadRepository novedades, Clock reloj) {
        this.novedades = novedades;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe la novedad
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si ya está CERRADA
     *                                      (NOV-06)
     */
    @Transactional
    public NovedadResult ejecutar(AvanzarNovedadCommand comando) {
        NovedadId id = new NovedadId(comando.id());
        Novedad novedad = novedades.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("la novedad", id.valor()));

        novedad.avanzar(new UsuarioId(comando.autor()), LocalDateTime.now(reloj));

        novedades.guardar(novedad);
        return NovedadResult.de(novedad);
    }
}
