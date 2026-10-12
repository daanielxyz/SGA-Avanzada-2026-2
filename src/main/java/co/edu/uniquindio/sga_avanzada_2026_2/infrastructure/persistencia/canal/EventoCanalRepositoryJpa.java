package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Adaptador JPA del puerto {@link EventoCanalRepository} (DEC-13): la bitácora solo agrega (BIT-02).
 */
@Repository
@Transactional
public class EventoCanalRepositoryJpa implements EventoCanalRepository {

    private final EventoCanalJpaRepository filas;

    public EventoCanalRepositoryJpa(EventoCanalJpaRepository filas) {
        this.filas = filas;
    }

    /**
     * Agrega un evento. Uno ya registrado nunca se sobrescribe (BIT-02).
     *
     * @throws ReglaDominioException si ya existe un evento con ese id
     */
    @Override
    public void registrar(EventoCanal evento) {
        if (filas.existsById(evento.id().valor())) {
            throw new ReglaDominioException("El evento " + evento.id().valor() + " ya está en la bitácora");
        }
        filas.save(CanalMapper.aJpa(evento));
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<EventoCanal> listarPorCanal(CanalId canalId, LocalDateTime desde, LocalDateTime hasta,
                                              int numeroPagina) {
        Page<EventoCanalJpa> pagina =
                filas.findByCanalIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(
                        canalId.valor(), desde, hasta, PageRequest.of(numeroPagina, Pagina.TAMANO));
        return new Pagina<>(pagina.getContent().stream().map(CanalMapper::aDominio).toList(), numeroPagina,
                Pagina.TAMANO, pagina.getTotalElements());
    }
}
