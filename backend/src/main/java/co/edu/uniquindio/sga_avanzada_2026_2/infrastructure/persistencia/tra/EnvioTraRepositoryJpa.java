package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EnvioTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EnvioTraRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EstadoEnvioTra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador JPA del puerto {@link EnvioTraRepository} (DEC-13 · DEC-130).
 */
@Repository
@Transactional
public class EnvioTraRepositoryJpa implements EnvioTraRepository {

    private final EnvioTraJpaRepository filas;

    public EnvioTraRepositoryJpa(EnvioTraJpaRepository filas) {
        this.filas = filas;
    }

    /** Inserta o actualiza reutilizando la fila cargada, para conservar su {@code version} (DEC-17). */
    @Override
    public void guardar(EnvioTra envio) {
        EnvioTraJpa fila = filas.findById(envio.reserva().valor()).orElseGet(EnvioTraJpa::new);
        fila.setReservaCodigo(envio.reserva().valor());
        fila.setEstado(envio.estado());
        fila.setCodigoPrincipal(envio.codigoPrincipal());
        fila.setIntentos(envio.intentos());
        fila.setUltimoIntento(envio.ultimoIntento());
        fila.setUltimoError(envio.ultimoError());
        fila.setEnviadoEn(envio.enviadoEn());
        fila.getAcompanantesEnviados().clear();
        fila.getAcompanantesEnviados().addAll(
                envio.acompanantesEnviados().stream().map(OcupanteId::valor).collect(Collectors.toSet()));
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnvioTra> buscarPorReserva(ReservaId reserva) {
        return filas.findById(reserva.valor()).map(EnvioTraRepositoryJpa::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaId> buscarParaEnviar(int maximoIntentos) {
        return filas.buscarParaEnviar(EstadoEnvioTra.PENDIENTE, EstadoEnvioTra.CON_ERROR, maximoIntentos).stream()
                .map(ReservaId::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<EnvioTra> listarPorEstado(EstadoEnvioTra estado, int numeroPagina) {
        Page<EnvioTraJpa> pagina = filas.findByEstadoOrderByUltimoIntentoDescReservaCodigoDesc(estado,
                PageRequest.of(numeroPagina, Pagina.TAMANO));
        return new Pagina<>(pagina.getContent().stream().map(EnvioTraRepositoryJpa::aDominio).toList(), numeroPagina,
                Pagina.TAMANO, pagina.getTotalElements());
    }

    private static EnvioTra aDominio(EnvioTraJpa fila) {
        return new EnvioTra(new ReservaId(fila.getReservaCodigo()), fila.getEstado(), fila.getCodigoPrincipal(),
                fila.getAcompanantesEnviados().stream().map(OcupanteId::new).collect(Collectors.toSet()),
                fila.getIntentos(), fila.getUltimoIntento(), fila.getUltimoError(), fila.getEnviadoEn());
    }
}
