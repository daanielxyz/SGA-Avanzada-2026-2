package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.GravedadNovedad;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Tabla del agregado Novedad. Su historial de pasos se carga siempre con ella (DEC-15 · DEC-50).
 */
@Entity
@Table(name = "novedad")
@Getter
@Setter
@NoArgsConstructor
public class NovedadJpa {

    @Id
    private String id;

    private String apartamentoCodigo;
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private GravedadNovedad gravedad;

    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "novedad_cambio", joinColumns = @JoinColumn(name = "novedad_id"))
    @OrderColumn(name = "orden")
    private List<CambioNovedadJpa> historial = new ArrayList<>();
}
