package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EstadoConflicto;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tabla del agregado ConflictoCanal. Nunca se borra una fila (CONF-05).
 */
@Entity
@Table(name = "conflicto_canal")
@Getter
@Setter
@NoArgsConstructor
public class ConflictoCanalJpa {

    @Id
    private String id;

    private String canalId;
    private String idExterno;
    private String apartamentoCodigo;
    private LocalDate entrada;
    private LocalDate salida;
    private LocalDateTime detectadoEn;
    private String motivo;

    @Enumerated(EnumType.STRING)
    private EstadoConflicto estado;

    private String resueltoPor;
    private LocalDateTime resueltoEn;
    private String decision;

    @Version
    private Long version;
}
