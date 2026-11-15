package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EstadoEnvioTra;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Tabla del agregado EnvioTra (DEC-130): una fila por reserva, sin los datos personales enviados.
 */
@Entity
@Table(name = "envio_tra")
@Getter
@Setter
@NoArgsConstructor
public class EnvioTraJpa {

    @Id
    private String reservaCodigo;

    @Enumerated(EnumType.STRING)
    private EstadoEnvioTra estado;

    private String codigoPrincipal;
    private int intentos;
    private LocalDateTime ultimoIntento;
    private String ultimoError;
    private LocalDateTime enviadoEn;

    @Version
    private Long version;

    /** Los acompañantes que ya entraron al Ministerio. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "envio_tra_acompanante", joinColumns = @JoinColumn(name = "reserva_codigo"))
    @Column(name = "ocupante_id")
    private Set<String> acompanantesEnviados = new HashSet<>();
}
