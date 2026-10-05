package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;

import java.time.LocalDate;
import java.util.List;

/**
 * Puerto de persistencia del agregado Tarifa. Las versiones no se modifican: cada una se guarda una vez (TAR-05).
 */
public interface TarifaRepository {

    void guardar(Tarifa tarifa);

    /**
     * Por cada temporada, la versión más alta del apartamento que ya rige en {@code fecha}: la vigente para cotizar,
     * reservar o modificar ese día (TAR-05 · TAR-06). Una versión con vigencia futura todavía no aplica.
     *
     * @param fecha fecha de la operación (hoy), no la de la estancia
     */
    List<Tarifa> buscarVigentesPorApartamento(ApartamentoId apartamentoId, LocalDate fecha);
}
