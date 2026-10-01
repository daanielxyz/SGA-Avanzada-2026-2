package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.*;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Entidad Raíz: Agregado ConflictoCanal (gestión de overbooking o disputas externas).
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

    private ConflictoCanal(ConflictoId id, CanalId canalId, String idExterno,
                           ApartamentoId apartamentoId, LocalDate fechaInicio, LocalDate fechaFin,
                           EstadoConflicto estado, String resolucion) {
        this.id = id;
        this.canalId = canalId;
        this.idExterno = idExterno;
        this.apartamentoId = apartamentoId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.resolucion = resolucion;
    }

    public static ConflictoCanal crear(ConflictoId id, CanalId canalId, String idExterno,
                                       ApartamentoId apartamentoId, LocalDate fechaInicio, LocalDate fechaFin) {
        Objects.requireNonNull(id, "El id del conflicto no puede ser nulo");
        Objects.requireNonNull(canalId, "El canalId no puede ser nulo");
        if (idExterno == null || idExterno.isBlank()) {
            throw new ReglaDominioException("El id externo no puede estar vacío");
        }
        Objects.requireNonNull(apartamentoId, "El apartamentoId no puede ser nulo");
        Objects.requireNonNull(fechaInicio, "La fecha de inicio no puede ser nula");
        Objects.requireNonNull(fechaFin, "La fecha de fin no puede ser nula");
        if (fechaFin.isBefore(fechaInicio)) {
            throw new ReglaDominioException("La fecha fin no puede ser anterior a la fecha de inicio");
        }

        return new ConflictoCanal(id, canalId, idExterno.trim(), apartamentoId,
                fechaInicio, fechaFin, EstadoConflicto.PENDIENTE, null);
    }

    public void resolver(UsuarioId autor, String decision) {
        Objects.requireNonNull(autor, "El autor de la resolución no puede ser nulo");
        if (decision == null || decision.isBlank()) {
            throw new ReglaDominioException("La decisión de resolución no puede estar vacía");
        }
        if (this.estado != EstadoConflicto.PENDIENTE) {
            throw new ReglaDominioException("El conflicto ya se encuentra resuelto");
        }
        this.estado = EstadoConflicto.RESUELTO;
        this.resolucion = "Resuelto por " + autor.valor() + ": " + decision.trim();
    }

    public ConflictoId getId() {
        return id;
    }

    public CanalId getCanalId() {
        return canalId;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public ApartamentoId getApartamentoId() {
        return apartamentoId;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public EstadoConflicto getEstado() {
        return estado;
    }

    public Optional<String> getResolucion() {
        return Optional.ofNullable(resolucion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConflictoCanal that = (ConflictoCanal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
