package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link CalendarioTemporadasRepository} (DEC-13).
 */
@Repository
@Transactional
public class CalendarioTemporadasRepositoryJpa implements CalendarioTemporadasRepository {

    private final CalendarioTemporadasJpaRepository filas;

    public CalendarioTemporadasRepositoryJpa(CalendarioTemporadasJpaRepository filas) {
        this.filas = filas;
    }

    /**
     * Inserta o actualiza reutilizando la fila cargada para conservar su {@code version} (DEC-17).
     */
    @Override
    public void guardar(CalendarioTemporadas calendario) {
        CalendarioTemporadasJpa fila = filas.findById(calendario.alojamientoId().valor())
                .orElseGet(CalendarioTemporadasJpa::new);
        CalendarioTemporadasMapper.copiar(calendario, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CalendarioTemporadas> buscarPorAlojamiento(AlojamientoId alojamientoId) {
        return filas.findById(alojamientoId.valor()).map(CalendarioTemporadasMapper::aDominio);
    }
}
