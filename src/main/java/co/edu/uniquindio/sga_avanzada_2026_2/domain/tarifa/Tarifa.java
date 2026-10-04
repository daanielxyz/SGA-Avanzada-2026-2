package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;

import java.time.LocalDate;

/**
 * Raíz del agregado Tarifa (una instancia por versión).
 */
// TODO(equipo): tarifa de Temporada Media por definir.
public class Tarifa {

    private final TarifaId id;
    private final ApartamentoId apartamentoId;
    private final TemporadaId temporadaId;
    private final Dinero valorPorOcupante;
    private final int version;
    private final LocalDate vigenteDesde;

    public Tarifa(TarifaId id, ApartamentoId apartamentoId, TemporadaId temporadaId, Dinero valorPorOcupante,
                  int version, LocalDate vigenteDesde) {
        this.id = id;
        this.apartamentoId = apartamentoId;
        this.temporadaId = temporadaId;
        this.valorPorOcupante = valorPorOcupante;
        this.version = version;
        this.vigenteDesde = vigenteDesde;
    }
}
