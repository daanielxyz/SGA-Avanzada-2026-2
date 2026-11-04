package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: los conflictos de canal que esperan la revisión del administrador (CONF-04). Es su lista de trabajo:
 * no se pagina.
 */
@Service
public class ConsultarConflictosPendientesUseCase {

    private final ConflictoCanalRepository conflictos;

    public ConsultarConflictosPendientesUseCase(ConflictoCanalRepository conflictos) {
        this.conflictos = conflictos;
    }

    @Transactional(readOnly = true)
    public List<ConflictoResult> ejecutar() {
        return conflictos.buscarPendientes().stream().map(ConflictoResult::de).toList();
    }
}
