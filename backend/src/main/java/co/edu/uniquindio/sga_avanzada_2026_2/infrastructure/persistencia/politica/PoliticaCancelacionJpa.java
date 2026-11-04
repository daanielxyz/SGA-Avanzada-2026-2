package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila de {@code politica_cancelacion}: una por versión. Sin {@code @Version} porque una versión nunca se modifica
 * (POL-04); la restricción única (alojamiento, versión) impide duplicarla.
 */
@Entity
@Table(name = "politica_cancelacion")
@Getter
@Setter
@NoArgsConstructor
public class PoliticaCancelacionJpa {

    @Id
    private String id;

    private String alojamientoId;
    private int version;

    @Enumerated(EnumType.STRING)
    private BaseRetencion noShowBase;

    private Integer noShowPorcentaje;
    private BigDecimal noShowMontoFijo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "politica_tramo", joinColumns = @JoinColumn(name = "politica_id"))
    @OrderColumn(name = "orden")
    private List<TramoJpa> tramos = new ArrayList<>();
}
