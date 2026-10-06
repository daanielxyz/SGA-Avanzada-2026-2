package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla del agregado Reserva. Registro y salida (0..1) van como columnas de la raíz; ocupantes y desglose se cargan
 * siempre con ella (DEC-15).
 */
@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
public class ReservaJpa {

    @Id
    private String codigo;

    private String apartamentoCodigo;
    private String titularId;
    private LocalDate entrada;
    private LocalDate salida;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    @Enumerated(EnumType.STRING)
    private CanalOrigen canalOrigen;

    private String canalId;
    private String idExterno;
    private LocalTime horaEstimadaLlegada;
    private BigDecimal valorTotal;
    private String politicaVersionId;
    private LocalDateTime creadaEn;

    private String registroId;
    private LocalDateTime registroFechaHora;
    private String registroAutor;
    private Boolean registroAnulado;

    private String salidaId;
    private LocalDateTime salidaFechaHora;
    private String salidaAutor;

    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reserva_ocupante", joinColumns = @JoinColumn(name = "reserva_codigo"))
    @OrderColumn(name = "orden")
    private List<OcupanteJpa> ocupantes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reserva_desglose_noche", joinColumns = @JoinColumn(name = "reserva_codigo"))
    @OrderColumn(name = "orden")
    private List<NocheReservaJpa> desglose = new ArrayList<>();
}
