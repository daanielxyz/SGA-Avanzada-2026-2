package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.alojamiento;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tabla del agregado Alojamiento. Los parámetros de negocio son columnas de la misma fila (DEC-25).
 */
@Entity
@Table(name = "alojamiento")
@Getter
@Setter
@NoArgsConstructor
public class AlojamientoJpa {

    @Id
    private String id;

    private String nombre;
    private String descripcion;
    private String ciudad;
    private String direccion;
    private double latitud;
    private double longitud;
    private String normas;

    private int umbralEdadFacturable;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;
    private int tiempoPreparacionMin;
    private int plazoConfirmacionMin;
    private LocalTime horaLimiteNoShow;
    private int anticipoPct;
    private int minimoMediosPago;
    private int minimoServiciosAdicionales;
    private int minimoTemporadas;
    private int minimoTramosCancelacion;

    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alojamiento_servicio_adicional", joinColumns = @JoinColumn(name = "alojamiento_id"))
    @OrderColumn(name = "orden")
    private List<ServicioAdicionalJpa> serviciosAdicionales = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alojamiento_medio_pago", joinColumns = @JoinColumn(name = "alojamiento_id"))
    @Column(name = "nombre")
    private Set<String> mediosPago = new LinkedHashSet<>();
}
