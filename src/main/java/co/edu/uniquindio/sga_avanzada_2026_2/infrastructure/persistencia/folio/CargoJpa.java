package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.SentidoAjuste;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.TipoCargo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila de {@code folio_cargo}. En el dominio Cargo es entidad interna; aquí basta un embebido porque solo existe
 * dentro de su folio.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CargoJpa {

    private String id;

    @Enumerated(EnumType.STRING)
    private TipoCargo tipo;

    private String concepto;
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    private SentidoAjuste sentido; // solo AJUSTE

    private LocalDate fecha;

    @Column(name = "corrige_a") // la estrategia de nombres no separa una mayúscula final
    private String corrigeA;
}
