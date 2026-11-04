package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.OperacionCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.SentidoEvento;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fila de {@code evento_canal}. Sin setters ni {@code @Version}: la bitácora es de solo escritura (BIT-02).
 */
@Entity
@Table(name = "evento_canal")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventoCanalJpa {

    @Id
    private String id;

    private String canalId;

    @Enumerated(EnumType.STRING)
    private OperacionCanal operacion;

    @Enumerated(EnumType.STRING)
    private SentidoEvento sentido;

    private LocalDateTime fechaHora;
    private String cargaUtil;

    @Enumerated(EnumType.STRING)
    private ResultadoEvento resultado;

    private String detalle;
}
