package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;

import java.time.LocalDate;

/**
 * Raíz del agregado ConflictoCanal: reserva externa rechazada por colisión (RN-18). Nunca se elimina.
 */
public class ConflictoCanal {

    private final ConflictoId id;
    private final CanalId canalId;
    private final String idExterno;
    private final ApartamentoId apartamentoId;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private EstadoConflicto estado;
    private String resolucion;

    public ConflictoCanal(ConflictoId id, CanalId canalId, String idExterno, ApartamentoId apartamentoId,
                          LocalDate fechaInicio, LocalDate fechaFin, EstadoConflicto estado, String resolucion) {
        this.id = id;
        this.canalId = canalId;
        this.idExterno = idExterno;
        this.apartamentoId = apartamentoId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.resolucion = resolucion;
    }
}
