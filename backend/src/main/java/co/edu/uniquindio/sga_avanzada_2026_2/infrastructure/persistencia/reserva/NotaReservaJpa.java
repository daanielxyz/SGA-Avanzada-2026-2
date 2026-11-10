package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tabla del agregado NotaReserva (BL-08 · DEC-105). {@code atendidaPor} y {@code atendidaEn} van juntas: nulas
 * mientras la nota está pendiente.
 */
@Entity
@Table(name = "nota_reserva")
@Getter
@Setter
@NoArgsConstructor
public class NotaReservaJpa {

    @Id
    private String id;

    private String reservaCodigo;
    private String texto;
    private String autor;
    private LocalDateTime escritaEn;
    private String atendidaPor;
    private LocalDateTime atendidaEn;

    @Version
    private Long version;
}
