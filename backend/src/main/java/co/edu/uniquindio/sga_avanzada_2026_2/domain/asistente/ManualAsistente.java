package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import java.util.List;

/**
 * Puerto del manual del PMS que usa el asistente (DEC-127): todas sus secciones, de todos los roles. Quién lee cuál
 * lo decide {@link Consulta}.
 */
public interface ManualAsistente {

    List<SeccionManual> secciones();
}
