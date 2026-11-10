package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.AtencionNota;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.NotaReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.NotaReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.NotaReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link NotaReservaRepository} (DEC-13). No borra filas (DEC-105).
 */
@Repository
@Transactional
public class NotaReservaRepositoryJpa implements NotaReservaRepository {

    private final NotaReservaJpaRepository filas;

    public NotaReservaRepositoryJpa(NotaReservaJpaRepository filas) {
        this.filas = filas;
    }

    /** Inserta o actualiza reutilizando la fila cargada, para conservar su {@code version} (DEC-17). */
    @Override
    public void guardar(NotaReserva nota) {
        NotaReservaJpa fila = filas.findById(nota.id().valor()).orElseGet(NotaReservaJpa::new);
        fila.setId(nota.id().valor());
        fila.setReservaCodigo(nota.reservaId().valor());
        fila.setTexto(nota.texto());
        fila.setAutor(nota.autor().valor());
        fila.setEscritaEn(nota.escritaEn());
        fila.setAtendidaPor(Optional.ofNullable(nota.atencion()).map(a -> a.autor().valor()).orElse(null));
        fila.setAtendidaEn(Optional.ofNullable(nota.atencion()).map(AtencionNota::fechaHora).orElse(null));
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotaReserva> buscarPorId(NotaReservaId id) {
        return filas.findById(id.valor()).map(NotaReservaRepositoryJpa::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotaReserva> listarPorReserva(ReservaId reservaId) {
        return filas.findByReservaCodigoOrderByEscritaEnDescIdDesc(reservaId.valor()).stream()
                .map(NotaReservaRepositoryJpa::aDominio)
                .toList();
    }

    /** Usa el constructor del dominio, que valida la nota (DEC-06). */
    private static NotaReserva aDominio(NotaReservaJpa fila) {
        AtencionNota atencion = fila.getAtendidaPor() == null ? null
                : new AtencionNota(new UsuarioId(fila.getAtendidaPor()), fila.getAtendidaEn());
        return new NotaReserva(new NotaReservaId(fila.getId()), new ReservaId(fila.getReservaCodigo()),
                fila.getTexto(), new UsuarioId(fila.getAutor()), fila.getEscritaEn(), atencion);
    }
}
