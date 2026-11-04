package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Temporada;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;

/**
 * Convierte CalendarioTemporadas ⇄ CalendarioTemporadasJpa usando el constructor de reconstrucción (DEC-06).
 */
final class CalendarioTemporadasMapper {

    private CalendarioTemporadasMapper() {
    }

    static CalendarioTemporadas aDominio(CalendarioTemporadasJpa jpa) {
        return new CalendarioTemporadas(
                new AlojamientoId(jpa.getAlojamientoId()),
                jpa.getTemporadas().stream()
                        .map(t -> new Temporada(new TemporadaId(t.getId()), t.getNombre(), t.getFechaInicio(),
                                t.getFechaFin(), t.isEsBase(), t.getEstanciaMinimaNoches(), t.isActiva()))
                        .toList());
    }

    /**
     * Copia el estado del dominio sobre una fila nueva o ya cargada; no toca {@code version} (DEC-17).
     */
    static void copiar(CalendarioTemporadas calendario, CalendarioTemporadasJpa jpa) {
        jpa.setAlojamientoId(calendario.alojamientoId().valor());
        jpa.getTemporadas().clear();
        calendario.temporadas().forEach(t -> jpa.getTemporadas().add(new TemporadaJpa(t.id().valor(), t.nombre(),
                t.fechaInicio(), t.fechaFin(), t.esBase(), t.estanciaMinimaNoches(), t.activa())));
    }
}
