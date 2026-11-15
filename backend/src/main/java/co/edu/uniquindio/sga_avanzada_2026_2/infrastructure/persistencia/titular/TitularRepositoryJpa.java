package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.BusquedaTitular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link TitularRepository} (DEC-13).
 */
@Repository
@Transactional
public class TitularRepositoryJpa implements TitularRepository {

    private static final Sort POR_NOMBRE = Sort.by("nombres", "primerApellido", "segundoApellido", "id");

    private final TitularJpaRepository filas;

    public TitularRepositoryJpa(TitularJpaRepository filas) {
        this.filas = filas;
    }

    /** Inserta o actualiza reutilizando la fila cargada, para conservar su {@code version} (DEC-17). */
    @Override
    public void guardar(Titular titular) {
        TitularJpa fila = filas.findById(titular.id().valor()).orElseGet(TitularJpa::new);
        TitularMapper.copiar(titular, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Titular> buscarPorId(TitularId id) {
        return filas.findById(id.valor()).map(TitularMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Titular> buscarPorIds(Collection<TitularId> ids) {
        return filas.findAllById(ids.stream().map(TitularId::valor).toList()).stream()
                .map(TitularMapper::aDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Titular> buscarPorDocumento(AlojamientoId alojamientoId, Documento documento) {
        return filas.findByAlojamientoIdAndTipoDocumentoAndNumeroDocumento(alojamientoId.valor(), documento.tipo(),
                        documento.numero())
                .map(TitularMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Titular> buscarPorCorreo(AlojamientoId alojamientoId, Correo correo) {
        return filas.findByAlojamientoIdAndCorreo(alojamientoId.valor(), correo.valor()).stream()
                .map(TitularMapper::aDominio)
                .toList();
    }

    /** Filtra y pagina en la base; por nombre y, entre iguales, por id, para que el orden no cambie entre páginas. */
    @Override
    @Transactional(readOnly = true)
    public Pagina<Titular> buscar(AlojamientoId alojamientoId, BusquedaTitular busqueda, int numeroPagina) {
        Page<TitularJpa> pagina = filas.findAll(BusquedaTitularJpa.de(alojamientoId.valor(), busqueda),
                PageRequest.of(numeroPagina, Pagina.TAMANO, POR_NOMBRE));
        return new Pagina<>(pagina.getContent().stream().map(TitularMapper::aDominio).toList(), numeroPagina,
                Pagina.TAMANO, pagina.getTotalElements());
    }
}
