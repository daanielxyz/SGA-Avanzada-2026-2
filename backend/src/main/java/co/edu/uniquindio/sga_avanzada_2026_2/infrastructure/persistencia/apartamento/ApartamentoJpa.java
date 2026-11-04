package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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
 * Tabla del agregado Apartamento. Los hijos se cargan siempre con la raíz (DEC-15).
 */
@Entity
@Table(name = "apartamento")
@Getter
@Setter
@NoArgsConstructor
public class ApartamentoJpa {

    @Id
    private String codigo;

    private String alojamientoId;
    private String nombre;
    private String descripcion;
    private int capacidad;
    private int dormitorios;

    @Enumerated(EnumType.STRING)
    private EstadoOperativo estadoOperativo;

    private boolean activo;

    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "apartamento_imagen", joinColumns = @JoinColumn(name = "apartamento_codigo"))
    @OrderColumn(name = "orden")
    private List<ImagenJpa> imagenes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "apartamento_caracteristica", joinColumns = @JoinColumn(name = "apartamento_codigo"))
    @OrderColumn(name = "orden")
    @Column(name = "nombre")
    private List<String> caracteristicas = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bloqueo", joinColumns = @JoinColumn(name = "apartamento_codigo"))
    @OrderColumn(name = "orden")
    private List<BloqueoJpa> bloqueos = new ArrayList<>();
}
