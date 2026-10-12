package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: el administrador ve los canales de su alojamiento, activos o no (CU-54). Son pocos: no se paginan.
 */
@Service
public class ListarCanales {

    private final CanalRepository canales;

    public ListarCanales(CanalRepository canales) {
        this.canales = canales;
    }

    @Transactional(readOnly = true)
    public List<CanalResult> ejecutar(String alojamientoId) {
        return canales.buscarPorAlojamiento(new AlojamientoId(alojamientoId)).stream()
                .map(CanalResult::de)
                .toList();
    }
}
