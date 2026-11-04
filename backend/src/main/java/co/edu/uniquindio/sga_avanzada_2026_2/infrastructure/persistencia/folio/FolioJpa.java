package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.folio;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla del agregado Folio. Cargos y pagos se cargan siempre con la raíz (DEC-15); el saldo no se guarda (SLD-01).
 */
@Entity
@Table(name = "folio")
@Getter
@Setter
@NoArgsConstructor
public class FolioJpa {

    @Id
    private String id;

    private String reservaCodigo;
    private boolean cerrado;
    private String autorizacionAutor;
    private String autorizacionMotivo;
    private LocalDateTime autorizacionFechaHora;

    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "folio_cargo", joinColumns = @JoinColumn(name = "folio_id"))
    @OrderColumn(name = "orden")
    private List<CargoJpa> cargos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "folio_pago", joinColumns = @JoinColumn(name = "folio_id"))
    @OrderColumn(name = "orden")
    private List<PagoJpa> pagos = new ArrayList<>();
}
