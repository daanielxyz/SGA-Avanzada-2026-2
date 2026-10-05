package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Adaptador JPA del puerto {@link TarifaRepository} (DEC-13). Es tan simple que el mapeo vive aquí mismo.
 */
@Repository
@Transactional
public class TarifaRepositoryJpa implements TarifaRepository {

    private final TarifaJpaRepository filas;

    public TarifaRepositoryJpa(TarifaJpaRepository filas) {
        this.filas = filas;
    }

    /**
     * Inserta la versión. Una versión ya guardada no se reescribe (TAR-05): repetirla falla por la restricción única.
     */
    @Override
    public void guardar(Tarifa tarifa) {
        filas.save(new TarifaJpa(tarifa.id().valor(), tarifa.apartamentoId().valor(), tarifa.temporadaId().valor(),
                tarifa.valorPorOcupante().monto(), tarifa.version(), tarifa.vigenteDesde()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tarifa> buscarVigentesPorApartamento(ApartamentoId apartamentoId, LocalDate fecha) {
        return filas.buscarVigentes(apartamentoId.valor(), fecha).stream()
                .map(t -> new Tarifa(new TarifaId(t.getId()), new ApartamentoId(t.getApartamentoCodigo()),
                        new TemporadaId(t.getTemporadaId()), new Dinero(t.getValorPorOcupante()), t.getVersion(),
                        t.getVigenteDesde()))
                .toList();
    }
}
