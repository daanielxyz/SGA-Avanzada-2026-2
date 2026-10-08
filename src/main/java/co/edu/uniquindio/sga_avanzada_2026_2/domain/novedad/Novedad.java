package co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado Novedad: daño, faltante o situación encontrada en un apartamento (NOV-01). No cambia por sí sola
 * el estado operativo ni crea bloqueos (NOV-03). No se elimina, se cierra (NOV-06); su historial guarda autor y fecha
 * de cada paso (NOV-04 · DEC-50).
 */
public class Novedad {

    private final NovedadId id;
    private final ApartamentoId apartamentoId;
    private final String descripcion;
    private final GravedadNovedad gravedad;
    private final List<CambioEstadoNovedad> historial; // el primero es el registro; el último, el estado actual

    // NOV-01 · NOV-04
    public Novedad(NovedadId id, ApartamentoId apartamentoId, String descripcion, GravedadNovedad gravedad,
                   List<CambioEstadoNovedad> historial) {
        if (id == null || apartamentoId == null || gravedad == null || historial == null || historial.isEmpty()) {
            throw new ReglaDominioException("La novedad requiere id, apartamento, gravedad e historial");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new ReglaDominioException("La descripción de la novedad es obligatoria");
        }
        for (int i = 0; i < historial.size(); i++) {
            if (historial.get(i).estado() != EstadoNovedad.values()[i]) {
                throw new ReglaDominioException("El historial de la novedad no sigue ABIERTA → EN_REVISION → CERRADA");
            }
        }
        this.id = id;
        this.apartamentoId = apartamentoId;
        this.descripcion = descripcion.trim();
        this.gravedad = gravedad;
        this.historial = new ArrayList<>(historial);
    }

    /**
     * Registra una novedad ABIERTA con su fecha y autor (NOV-01 · NOV-04). Puede hacerlo el personal de servicio
     * (NOV-05); no toca el apartamento (NOV-03).
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si falta la descripción u otro dato (NOV-01)
     */
    public static Novedad registrar(NovedadId id, ApartamentoId apartamentoId, String descripcion,
                                    GravedadNovedad gravedad, UsuarioId autor, LocalDateTime ahora) {
        return new Novedad(id, apartamentoId, descripcion, gravedad,
                List.of(new CambioEstadoNovedad(EstadoNovedad.ABIERTA, autor, ahora)));
    }

    /**
     * Avanza un paso del ciclo (ABIERTA → EN_REVISION → CERRADA) dejando autor y fecha (NOV-04 · CU-55).
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si ya está cerrada (NOV-06) o la fecha es anterior al paso anterior
     */
    public void avanzar(UsuarioId autor, LocalDateTime ahora) {
        Objects.requireNonNull(ahora, "ahora");
        CambioEstadoNovedad actual = historial.getLast();
        EstadoNovedad siguiente = actual.estado().siguiente();
        if (ahora.isBefore(actual.fechaHora())) {
            throw new ReglaDominioException("Un paso de la novedad no puede ser anterior al anterior");
        }
        historial.add(new CambioEstadoNovedad(siguiente, autor, ahora));
    }

    public EstadoNovedad estado() {
        return historial.getLast().estado();
    }

    public UsuarioId autor() {
        return historial.getFirst().autor();
    }

    public LocalDateTime registradaEn() {
        return historial.getFirst().fechaHora();
    }

    public NovedadId id() {
        return id;
    }

    public ApartamentoId apartamentoId() {
        return apartamentoId;
    }

    public String descripcion() {
        return descripcion;
    }

    public GravedadNovedad gravedad() {
        return gravedad;
    }

    public List<CambioEstadoNovedad> historial() {
        return List.copyOf(historial);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Novedad otra && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
