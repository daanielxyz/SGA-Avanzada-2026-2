package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.GravedadNovedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.Novedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: reportar un daño, un faltante o una situación encontrada en un apartamento (CU-43). No cambia el
 * estado operativo ni bloquea el apartamento (NOV-03): solo se guarda la Novedad.
 */
@Service
public class RegistrarNovedadUseCase {

    private final NovedadRepository novedades;
    private final ApartamentoRepository apartamentos;
    private final GeneradorCodigos codigos;
    private final Clock reloj;

    public RegistrarNovedadUseCase(NovedadRepository novedades, ApartamentoRepository apartamentos,
                                   GeneradorCodigos codigos, Clock reloj) {
        this.novedades = novedades;
        this.apartamentos = apartamentos;
        this.codigos = codigos;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el apartamento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si falta la descripción
     *                                      (NOV-01)
     */
    @Transactional
    public NovedadResult ejecutar(RegistrarNovedadCommand comando) {
        ApartamentoId apartamentoId = new ApartamentoId(comando.apartamento());
        apartamentos.buscarPorCodigo(apartamentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", apartamentoId.valor()));

        Novedad novedad = Novedad.registrar(new NovedadId(codigos.siguiente(SerieCodigo.NOVEDAD)), apartamentoId,
                comando.descripcion(), GravedadNovedad.valueOf(comando.gravedad()), new UsuarioId(comando.autor()),
                LocalDateTime.now(reloj));

        novedades.guardar(novedad);
        return NovedadResult.de(novedad);
    }
}
