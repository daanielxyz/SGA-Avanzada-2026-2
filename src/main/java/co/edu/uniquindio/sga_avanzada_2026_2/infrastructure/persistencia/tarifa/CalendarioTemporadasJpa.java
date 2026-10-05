package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tarifa;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabla del agregado CalendarioTemporadas; sus temporadas se cargan siempre con él (DEC-15).
 */
@Entity
@Table(name = "calendario_temporadas")
@Getter
@Setter
@NoArgsConstructor
public class CalendarioTemporadasJpa {

    @Id
    private String alojamientoId;

    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "temporada", joinColumns = @JoinColumn(name = "alojamiento_id"))
    @OrderColumn(name = "orden")
    private List<TemporadaJpa> temporadas = new ArrayList<>();
}
