package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador desactiva un canal, que deja de traer reservas nuevas y conserva las históricas
 * (CU-54 · CAN-05).
 */
@Service
public class DesactivarCanalUseCase {

    private final CanalRepository canales;

    public DesactivarCanalUseCase(CanalRepository canales) {
        this.canales = canales;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el canal
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si ya estaba inactivo
     */
    @Transactional
    public CanalResult ejecutar(String id) {
        CanalId canalId = new CanalId(id);
        Canal canal = canales.buscarPorId(canalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el canal", canalId.valor()));

        canal.desactivar();

        canales.guardar(canal);
        return CanalResult.de(canal);
    }
}
