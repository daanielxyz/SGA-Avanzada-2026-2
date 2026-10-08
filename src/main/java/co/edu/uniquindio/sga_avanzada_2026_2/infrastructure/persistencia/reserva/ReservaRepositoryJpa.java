package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link ReservaRepository} (DEC-13).
 */
@Repository
@Transactional
public class ReservaRepositoryJpa implements ReservaRepository {

    // EDO-03: la lista sale del enum, no se repite aquí
    private static final List<EstadoReserva> ACTIVOS = Arrays.stream(EstadoReserva.values())
            .filter(EstadoReserva::retieneDisponibilidad)
            .toList();

    private final ReservaJpaRepository filas;

    public ReservaRepositoryJpa(ReservaJpaRepository filas) {
        this.filas = filas;
    }

    /**
     * Inserta o actualiza. Si la fila ya existe se reutiliza para conservar su {@code version}: dentro de la
     * transacción del caso de uso es la misma instancia que se cargó, y un cambio concurrente provoca un
     * conflicto de bloqueo optimista en lugar de sobrescribirse (DEC-17).
     */
    @Override
    public void guardar(Reserva reserva) {
        ReservaJpa fila = filas.findById(reserva.codigo().valor()).orElseGet(ReservaJpa::new);
        ReservaMapper.copiar(reserva, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reserva> buscarPorCodigo(ReservaId codigo) {
        return filas.findById(codigo.valor()).map(ReservaMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> buscarActivasPorApartamento(ApartamentoId apartamentoId) {
        return filas.findByApartamentoCodigoAndEstadoIn(apartamentoId.valor(), ACTIVOS).stream()
                .map(ReservaMapper::aDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reserva> buscarPorCanalEIdExterno(CanalId canalId, String idExterno) {
        return filas.findByCanalIdAndIdExterno(canalId.valor(), idExterno).map(ReservaMapper::aDominio);
    }
}
