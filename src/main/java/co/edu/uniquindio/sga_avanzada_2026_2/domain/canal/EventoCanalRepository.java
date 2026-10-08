package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Puerto de la bitácora de canales: solo agrega y consulta; no hay actualización ni eliminación (BIT-02).
 */
public interface EventoCanalRepository {

    void registrar(EventoCanal evento);

    /** Eventos de un canal en [desde, hasta), del más reciente al más antiguo (CU-45). */
    List<EventoCanal> buscarPorCanal(CanalId canalId, LocalDateTime desde, LocalDateTime hasta);
}
