package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Entidad Raíz: Agregado Reserva.
 */
public class Reserva {

    private final ReservaId codigo;
    private ApartamentoId apartamentoId;
    private final TitularId titularId;
    private Estancia estancia;
    private EstadoReserva estado;
    private final CanalOrigen canalOrigen;
    private final CanalId canalId;
    private final String idExterno;
    private final List<Ocupante> ocupantes;
    private final Hora horaEstimadaLlegada;
    private final Dinero valorTotal;
    private final PoliticaId politicaVersionId;
    private final LocalDateTime fechaCreacion;

    private Reserva(ReservaId codigo, ApartamentoId apartamentoId, TitularId titularId,
                    Estancia estancia, EstadoReserva estado, CanalOrigen canalOrigen,
                    CanalId canalId, String idExterno, List<Ocupante> ocupantes,
                    Hora horaEstimadaLlegada, Dinero valorTotal, PoliticaId politicaVersionId,
                    LocalDateTime fechaCreacion) {
        this.codigo = codigo;
        this.apartamentoId = apartamentoId;
        this.titularId = titularId;
        this.estancia = estancia;
        this.estado = estado;
        this.canalOrigen = canalOrigen;
        this.canalId = canalId;
        this.idExterno = idExterno;
        this.ocupantes = new ArrayList<>(ocupantes);
        this.horaEstimadaLlegada = horaEstimadaLlegada;
        this.valorTotal = valorTotal;
        this.politicaVersionId = politicaVersionId;
        this.fechaCreacion = fechaCreacion;
    }

    public static Reserva crear(ReservaId codigo, ApartamentoId apartamentoId, TitularId titularId,
                                 Estancia estancia, CanalOrigen canalOrigen, CanalId canalId,
                                 String idExterno, List<Ocupante> ocupantes, Hora horaEstimadaLlegada,
                                 Dinero valorTotal, PoliticaId politicaVersionId, LocalDate fechaHoy) {
        Objects.requireNonNull(codigo, "El código de reserva no puede ser nulo");
        Objects.requireNonNull(apartamentoId, "El apartamentoId no puede ser nulo");
        Objects.requireNonNull(titularId, "El titularId no puede ser nulo");
        Objects.requireNonNull(estancia, "La estancia no puede ser nula");
        Objects.requireNonNull(canalOrigen, "El canal de origen no puede ser nulo");
        Objects.requireNonNull(ocupantes, "La lista de ocupantes no puede ser nula");
        if (ocupantes.isEmpty()) {
            throw new ReglaDominioException("La reserva debe contar con al menos un ocupante");
        }
        Objects.requireNonNull(horaEstimadaLlegada, "La hora estimada de llegada no puede ser nula");
        Objects.requireNonNull(valorTotal, "El valor total no puede ser nulo");
        Objects.requireNonNull(politicaVersionId, "La política de versión no puede ser nula");
        Objects.requireNonNull(fechaHoy, "La fecha de hoy no puede ser nula");

        if (estancia.entrada().isBefore(fechaHoy)) {
            throw new ReglaDominioException("La fecha de entrada no puede ser anterior a la fecha actual");
        }

        if (canalOrigen == CanalOrigen.EXTERNO && canalId == null) {
            throw new ReglaDominioException("El canalId es obligatorio cuando el canal de origen es EXTERNO");
        }

        return new Reserva(codigo, apartamentoId, titularId, estancia, EstadoReserva.PENDIENTE,
                canalOrigen, canalId, idExterno, ocupantes, horaEstimadaLlegada,
                valorTotal, politicaVersionId, LocalDateTime.now());
    }

    public void agregarOcupante(Ocupante o) {
        Objects.requireNonNull(o, "El ocupante no puede ser nulo");
        if (!this.estado.estaActiva()) {
            throw new ReglaDominioException("No se pueden agregar ocupantes a una reserva que no está activa");
        }
        this.ocupantes.add(o);
    }

    public void modificar(Estancia nuevaEstancia, List<Ocupante> nuevosOcupantes, ApartamentoId nuevoAptoId) {
        if (this.estado != EstadoReserva.PENDIENTE && this.estado != EstadoReserva.CONFIRMADA) {
            throw new ReglaDominioException("Solo se pueden modificar reservas en estado PENDIENTE o CONFIRMADA");
        }
        Objects.requireNonNull(nuevaEstancia, "La nueva estancia no puede ser nula");
        Objects.requireNonNull(nuevosOcupantes, "La lista de ocupantes no puede ser nula");
        if (nuevosOcupantes.isEmpty()) {
            throw new ReglaDominioException("La reserva debe contener al menos un ocupante");
        }
        Objects.requireNonNull(nuevoAptoId, "El nuevo apartamentoId no puede ser nulo");

        this.estancia = nuevaEstancia;
        this.ocupantes.clear();
        this.ocupantes.addAll(nuevosOcupantes);
        this.apartamentoId = nuevoAptoId;
    }

    public void confirmar() {
        if (!this.estado.puedePasarA(EstadoReserva.CONFIRMADA)) {
            throw new ReglaDominioException("No se puede confirmar la reserva desde el estado " + this.estado);
        }
        this.estado = EstadoReserva.CONFIRMADA;
    }

    public void cancelar(LocalDateTime ahora) {
        Objects.requireNonNull(ahora, "La fecha y hora actual no pueden ser nulas");
        if (!this.estado.puedePasarA(EstadoReserva.CANCELADA)) {
            throw new ReglaDominioException("No se puede cancelar la reserva desde el estado " + this.estado);
        }
        this.estado = EstadoReserva.CANCELADA;
    }

    public void declararNoShow(LocalDateTime ahora, LocalTime horaLimite) {
        Objects.requireNonNull(ahora, "La fecha y hora actual no pueden ser nulas");
        Objects.requireNonNull(horaLimite, "La hora límite no puede ser nula");
        if (!this.estado.puedePasarA(EstadoReserva.NO_SHOW)) {
            throw new ReglaDominioException("No se puede declarar NO_SHOW desde el estado " + this.estado);
        }
        LocalDateTime limite = estancia.entrada().atTime(horaLimite);
        if (ahora.isBefore(limite)) {
            throw new ReglaDominioException("No se puede declarar NO_SHOW antes de la hora límite: " + limite);
        }
        this.estado = EstadoReserva.NO_SHOW;
    }

    public void expirar(LocalDateTime ahora, Duration plazo) {
        Objects.requireNonNull(ahora, "La fecha y hora actual no pueden ser nulas");
        Objects.requireNonNull(plazo, "El plazo no puede ser nulo");
        if (this.estado == EstadoReserva.PENDIENTE) {
            LocalDateTime limite = fechaCreacion.plus(plazo);
            if (ahora.isAfter(limite)) {
                this.estado = EstadoReserva.CANCELADA;
            }
        }
    }

    public void registrarLlegada(LocalDate hoy) {
        Objects.requireNonNull(hoy, "La fecha de llegada no puede ser nula");
        if (!this.estado.puedePasarA(EstadoReserva.EN_CURSO)) {
            throw new ReglaDominioException("No se puede registrar llegada desde el estado " + this.estado);
        }
        if (hoy.isBefore(estancia.entrada())) {
            throw new ReglaDominioException("No se puede registrar llegada antes de la fecha de entrada");
        }
        this.estado = EstadoReserva.EN_CURSO;
    }

    public void registrarSalida() {
        if (!this.estado.puedePasarA(EstadoReserva.FINALIZADA)) {
            throw new ReglaDominioException("No se puede registrar salida desde el estado " + this.estado);
        }
        this.estado = EstadoReserva.FINALIZADA;
    }

    public boolean estaActiva() {
        return this.estado.estaActiva();
    }

    public boolean seSolapaCon(Estancia otraEstancia) {
        Objects.requireNonNull(otraEstancia, "La otra estancia no puede ser nula");
        return this.estaActiva() && this.estancia.seSolapaCon(otraEstancia);
    }

    public ReservaId getCodigo() {
        return codigo;
    }

    public ReservaId getId() {
        return codigo;
    }

    public ApartamentoId getApartamentoId() {
        return apartamentoId;
    }

    public TitularId getTitularId() {
        return titularId;
    }

    public Estancia getEstancia() {
        return estancia;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public CanalOrigen getCanalOrigen() {
        return canalOrigen;
    }

    public Optional<CanalId> getCanalId() {
        return Optional.ofNullable(canalId);
    }

    public Optional<String> getIdExterno() {
        return Optional.ofNullable(idExterno);
    }

    public List<Ocupante> getOcupantes() {
        return Collections.unmodifiableList(ocupantes);
    }

    public Hora getHoraEstimadaLlegada() {
        return horaEstimadaLlegada;
    }

    public Dinero valorTotal() {
        return valorTotal;
    }

    public Dinero getValorTotal() {
        return valorTotal;
    }

    public PoliticaId getPoliticaVersionId() {
        return politicaVersionId;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reserva reserva = (Reserva) o;
        return Objects.equals(codigo, reserva.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
