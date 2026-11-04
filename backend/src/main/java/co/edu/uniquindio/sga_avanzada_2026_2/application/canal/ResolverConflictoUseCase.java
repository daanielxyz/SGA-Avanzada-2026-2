package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: el administrador marca un conflicto de canal como revisado y deja la decisión que tomó (CONF-04). No
 * cambia ninguna reserva (CONF-02).
 */
@Service
public class ResolverConflictoUseCase {

    private final ConflictoCanalRepository conflictos;
    private final Clock reloj;

    public ResolverConflictoUseCase(ConflictoCanalRepository conflictos, Clock reloj) {
        this.conflictos = conflictos;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el conflicto
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si ya estaba resuelto o
     *                                      falta la decisión
     */
    @Transactional
    public ConflictoResult ejecutar(ResolverConflictoCommand comando) {
        ConflictoId id = new ConflictoId(comando.id());
        ConflictoCanal conflicto = conflictos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el conflicto de canal", id.valor()));

        conflicto.resolver(new UsuarioId(comando.autor()), comando.decision(), LocalDateTime.now(reloj));

        conflictos.guardar(conflicto);
        return ConflictoResult.de(conflicto);
    }
}
