package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.FiltroNovedades;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.Novedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link NovedadRepository} (DEC-13). No borra filas (NOV-06).
 */
@Repository
@Transactional
public class NovedadRepositoryJpa implements NovedadRepository {

    private final NovedadJpaRepository filas;

    public NovedadRepositoryJpa(NovedadJpaRepository filas) {
        this.filas = filas;
    }

    /** Inserta o actualiza reutilizando la fila cargada, para conservar su {@code version} (DEC-17). */
    @Override
    public void guardar(Novedad novedad) {
        NovedadJpa fila = filas.findById(novedad.id().valor()).orElseGet(NovedadJpa::new);
        NovedadMapper.copiar(novedad, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Novedad> buscarPorId(NovedadId id) {
        return filas.findById(id.valor()).map(NovedadMapper::aDominio);
    }

    /**
     * La fecha de registro vive en el historial, así que el orden y el corte de la página se aplican al cargar las
     * novedades del apartamento, que son pocas.
     */
    @Override
    @Transactional(readOnly = true)
    public Pagina<Novedad> listarPorApartamento(ApartamentoId apartamentoId, int numeroPagina) {
        List<Novedad> todas = filas.findByApartamentoCodigo(apartamentoId.valor()).stream()
                .map(NovedadMapper::aDominio)
                .sorted(Comparator.comparing(Novedad::registradaEn).reversed())
                .toList();
        List<Novedad> pagina = todas.stream()
                .skip((long) numeroPagina * Pagina.TAMANO)
                .limit(Pagina.TAMANO)
                .toList();
        return new Pagina<>(pagina, numeroPagina, Pagina.TAMANO, todas.size());
    }

    /**
     * Como en {@link #listarPorApartamento}, el estado y la fecha viven en el historial: se filtra, se ordena y se
     * corta en memoria. Un alojamiento tiene pocas novedades; si crecen, el estado y la fecha pasan a columnas
     * (DEC-132).
     */
    @Override
    @Transactional(readOnly = true)
    public Pagina<Novedad> listar(FiltroNovedades filtro, int numeroPagina) {
        return Pagina.de(filas.findAll().stream()
                .map(NovedadMapper::aDominio)
                .filter(filtro::cumple)
                .sorted(Comparator.comparing(Novedad::registradaEn).reversed()
                        .thenComparing(novedad -> novedad.id().valor()))
                .toList(), numeroPagina);
    }
}
