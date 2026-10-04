package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado Apartamento: única unidad reservable; sus dormitorios y áreas no se venden por separado
 * (APA-14). Las reglas que cruzan reservas o tarifas llegan resueltas por parámetro desde los servicios de dominio.
 */
public class Apartamento {

    private static final int MAXIMO_IMAGENES = 10;

    private final ApartamentoId codigo;
    private final AlojamientoId alojamientoId;
    private final String nombre;
    private final String descripcion;
    private Capacidad capacidad;
    private final Dormitorio dormitorios;
    private EstadoOperativo estadoOperativo;
    private final List<Imagen> imagenes;               // 0..10; sin imágenes no se puede activar
    private final List<Caracteristica> caracteristicas; // ≥1
    private final List<Bloqueo> bloqueos;
    private boolean activo;

    // IMG-01 · IMG-02 · CARAC-01
    public Apartamento(ApartamentoId codigo, AlojamientoId alojamientoId, String nombre, String descripcion,
                       Capacidad capacidad, Dormitorio dormitorios, EstadoOperativo estadoOperativo,
                       List<Imagen> imagenes, List<Caracteristica> caracteristicas, List<Bloqueo> bloqueos,
                       boolean activo) {
        if (codigo == null || alojamientoId == null || capacidad == null || dormitorios == null
                || estadoOperativo == null || imagenes == null || caracteristicas == null || bloqueos == null) {
            throw new ReglaDominioException("Faltan datos obligatorios del apartamento");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del apartamento es obligatorio");
        }
        if (imagenes.size() > MAXIMO_IMAGENES) {
            throw new ReglaDominioException("Un apartamento admite máximo " + MAXIMO_IMAGENES + " imágenes");
        }
        if (!imagenes.isEmpty() && imagenes.stream().filter(Imagen::principal).count() != 1) {
            throw new ReglaDominioException("Exactamente una imagen debe ser la principal");
        }
        if (caracteristicas.isEmpty()) {
            throw new ReglaDominioException("El apartamento debe declarar al menos una característica");
        }
        this.codigo = codigo;
        this.alojamientoId = alojamientoId;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.capacidad = capacidad;
        this.dormitorios = dormitorios;
        this.estadoOperativo = estadoOperativo;
        this.imagenes = List.copyOf(imagenes);
        this.caracteristicas = List.copyOf(caracteristicas);
        this.bloqueos = new ArrayList<>(bloqueos);
        this.activo = activo;
    }

    /**
     * Publica el apartamento para la venta.
     *
     * @param tarifasCompletas resultado de {@code ActivadorApartamentoService}: tiene tarifa en todas las
     *                         temporadas (APA-11 · TAR-03)
     * @throws ReglaDominioException si ya está activo, no tiene imágenes (IMG-01) o le faltan tarifas (TAR-03)
     */
    public void activar(boolean tarifasCompletas) {
        if (activo) {
            throw new ReglaDominioException("El apartamento " + codigo.valor() + " ya está activo");
        }
        if (imagenes.isEmpty()) {
            throw new ReglaDominioException("Un apartamento sin imágenes no puede publicarse");
        }
        if (!tarifasCompletas) {
            throw new ReglaDominioException("El apartamento necesita tarifa en todas las temporadas para activarse");
        }
        activo = true;
    }

    /**
     * Retira el apartamento de la venta (eliminación lógica). Las reservas históricas lo siguen referenciando (APA-16).
     *
     * @param hayReservasActivas si tiene reservas activas o futuras, según {@code BajaApartamentoDomainService}
     * @throws ReglaDominioException si ya estaba retirado o tiene reservas activas o futuras (APA-16)
     */
    public void retirarDeVenta(boolean hayReservasActivas) {
        if (!activo) {
            throw new ReglaDominioException("El apartamento " + codigo.valor() + " ya está retirado de la venta");
        }
        if (hayReservasActivas) {
            throw new ReglaDominioException("No se puede retirar un apartamento con reservas activas o futuras");
        }
        activo = false;
    }

    /**
     * Reemplaza la capacidad (CAP-04). No afecta reservas ya creadas; advertir y listar las afectadas es tarea
     * del caso de uso (APA-15).
     */
    public void cambiarCapacidad(Capacidad nueva) {
        capacidad = Objects.requireNonNull(nueva, "nueva");
    }

    /**
     * Cambia el estado operativo. No crea ni levanta bloqueos (BLO-05).
     *
     * @throws ReglaDominioException si la transición no está permitida (EOPE-02)
     */
    public void cambiarEstadoOperativo(EstadoOperativo destino) {
        if (!estadoOperativo.puedePasarA(destino)) {
            throw new ReglaDominioException(
                    "Transición de estado operativo no permitida: " + estadoOperativo + " → " + destino);
        }
        estadoOperativo = destino;
    }

    /**
     * Registra un bloqueo vigente [inicio, fin). Que no choque con reservas activas (BLO-01) lo verifica
     * {@code BloqueoOperativoDomainService} antes de invocarlo. No cambia el estado operativo (BLO-05).
     *
     * @throws ReglaDominioException si el id ya existe o el rango o motivo son inválidos (BLO-02)
     */
    public void registrarBloqueo(BloqueoId id, LocalDate inicio, LocalDate fin, String motivo) {
        Bloqueo nuevo = new Bloqueo(id, inicio, fin, motivo, true);
        if (bloqueos.contains(nuevo)) {
            throw new ReglaDominioException("Ya existe el bloqueo " + id.valor());
        }
        bloqueos.add(nuevo);
    }

    /**
     * Levanta un bloqueo: libera sus noches de inmediato y lo conserva como historial (BLO-06 · APA-12).
     *
     * @throws ReglaDominioException si el bloqueo no existe en este apartamento o ya fue levantado
     */
    public void levantarBloqueo(BloqueoId id) {
        bloqueos.stream()
                .filter(b -> b.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ReglaDominioException("El apartamento no tiene el bloqueo " + id.valor()))
                .levantar();
    }

    /**
     * Indica si algún bloqueo vigente impide vender la noche (RN-07 · BLO-03).
     */
    public boolean tieneBloqueoEn(Noche noche) {
        return bloqueos.stream().anyMatch(b -> b.cubre(noche));
    }

    /**
     * Tope rígido de ocupantes (RN-02 · CAP-02).
     *
     * @return {@code true} si el grupo cabe en el apartamento
     */
    public boolean admite(int ocupantes) {
        return capacidad.admite(ocupantes);
    }

    public ApartamentoId codigo() {
        return codigo;
    }

    public AlojamientoId alojamientoId() {
        return alojamientoId;
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }

    public Capacidad capacidad() {
        return capacidad;
    }

    public Dormitorio dormitorios() {
        return dormitorios;
    }

    public EstadoOperativo estadoOperativo() {
        return estadoOperativo;
    }

    public List<Imagen> imagenes() {
        return imagenes;
    }

    public List<Caracteristica> caracteristicas() {
        return caracteristicas;
    }

    public List<Bloqueo> bloqueos() {
        return List.copyOf(bloqueos);
    }

    public boolean activo() {
        return activo;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Apartamento otro && codigo.equals(otro.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }
}
