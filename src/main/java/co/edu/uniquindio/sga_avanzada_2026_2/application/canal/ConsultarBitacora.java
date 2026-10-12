package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador consulta la bitácora de un canal entre dos días, del evento más reciente al más
 * antiguo y de 10 en 10 (CU-45 · DEC-18).
 */
@Service
public class ConsultarBitacora {

    private final EventoCanalRepository eventos;
    private final CanalRepository canales;

    public ConsultarBitacora(EventoCanalRepository eventos, CanalRepository canales) {
        this.eventos = eventos;
        this.canales = canales;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el canal
     */
    @Transactional(readOnly = true)
    public Pagina<EventoCanalResult> ejecutar(ConsultarBitacoraCommand comando) {
        CanalId canalId = new CanalId(comando.canalId());
        canales.buscarPorId(canalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el canal", canalId.valor()));
        return eventos.listarPorCanal(canalId, comando.desde().atStartOfDay(),
                        comando.hasta().plusDays(1).atStartOfDay(), comando.numeroPagina())
                .map(EventoCanalResult::de);
    }
}
