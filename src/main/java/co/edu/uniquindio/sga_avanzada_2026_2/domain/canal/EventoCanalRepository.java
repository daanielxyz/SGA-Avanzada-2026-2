package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;

import java.time.LocalDateTime;

/**
 * Puerto de la bitácora de canales: solo agrega y consulta; no hay actualización ni eliminación (BIT-02).
 */
public interface EventoCanalRepository {

    void registrar(EventoCanal evento);

    /**
     * Eventos de un canal en [desde, hasta), del más reciente al más antiguo, de 10 en 10 (CU-45 · DEC-18).
     *
     * @param numeroPagina empieza en 0
     */
    Pagina<EventoCanal> listarPorCanal(CanalId canalId, LocalDateTime desde, LocalDateTime hasta, int numeroPagina);
}
