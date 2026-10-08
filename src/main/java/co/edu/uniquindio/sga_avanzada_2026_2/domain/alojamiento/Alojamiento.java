package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado Alojamiento: datos del establecimiento, su configuración de negocio, catálogo de medios de
 * pago habilitados y servicios adicionales. Que exista uno solo (ALO-01) es regla de despliegue, no del modelo.
 */
public class Alojamiento {

    private final AlojamientoId id;
    private final String nombre;
    private final String descripcion;
    private final String ciudad;
    private final String direccion;
    private Ubicacion ubicacion;
    private final String normas; // opcional
    private ParametrosAlojamiento parametros;
    private final List<ServicioAdicional> serviciosAdicionales;
    private final List<MedioPago> mediosPago; // solo los habilitados

    // ALO-02 · ALO-06 · MPAG-01 · SERV-05
    public Alojamiento(AlojamientoId id, String nombre, String descripcion, String ciudad, String direccion,
                       Ubicacion ubicacion, String normas, ParametrosAlojamiento parametros,
                       List<ServicioAdicional> serviciosAdicionales, List<MedioPago> mediosPago) {
        if (id == null || ubicacion == null || parametros == null || serviciosAdicionales == null
                || mediosPago == null) {
            throw new ReglaDominioException("Faltan datos obligatorios del alojamiento");
        }
        if (vacio(nombre) || vacio(descripcion) || vacio(ciudad) || vacio(direccion)) {
            throw new ReglaDominioException("Nombre, descripción, ciudad y dirección del alojamiento son obligatorios");
        }
        if (new HashSet<>(mediosPago).size() != mediosPago.size()) {
            throw new ReglaDominioException("Hay medios de pago repetidos");
        }
        if (serviciosAdicionales.stream().map(ServicioAdicional::id).distinct().count() != serviciosAdicionales.size()) {
            throw new ReglaDominioException("Hay servicios adicionales con id repetido");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.descripcion = descripcion.trim();
        this.ciudad = ciudad.trim();
        this.direccion = direccion.trim();
        this.ubicacion = ubicacion;
        this.normas = normas;
        this.parametros = parametros;
        this.serviciosAdicionales = new ArrayList<>(serviciosAdicionales);
        this.mediosPago = new ArrayList<>(mediosPago);
        validarMinimos(parametros, this.mediosPago.size(), serviciosActivos());
    }

    /**
     * Crea el alojamiento del despliegue con sus valores iniciales (DEC-25 · DEC-34). No hay reglas de nacimiento
     * aparte de las invariantes, que valida el constructor; que exista uno solo es regla de despliegue (ALO-01).
     *
     * @throws ReglaDominioException si falta un dato obligatorio (ALO-02) o el catálogo no alcanza los mínimos
     *                               (ALO-06 · MPAG-01 · SERV-05)
     */
    public static Alojamiento crear(AlojamientoId id, String nombre, String descripcion, String ciudad,
                                    String direccion, Ubicacion ubicacion, String normas,
                                    ParametrosAlojamiento parametros, List<ServicioAdicional> serviciosAdicionales,
                                    List<MedioPago> mediosPago) {
        return new Alojamiento(id, nombre, descripcion, ciudad, direccion, ubicacion, normas, parametros,
                serviciosAdicionales, mediosPago);
    }

    /**
     * Reemplaza la configuración de negocio; aplica a operaciones futuras, no a reservas ya creadas (ALO-03 · ALO-04).
     *
     * @throws ReglaDominioException si los nuevos mínimos superan el catálogo actual (ALO-06)
     */
    public void cambiarParametros(ParametrosAlojamiento nuevos) {
        Objects.requireNonNull(nuevos, "nuevos");
        validarMinimos(nuevos, mediosPago.size(), serviciosActivos());
        parametros = nuevos;
    }

    /**
     * Reemplaza la ubicación completa, sin historial (UBI-03).
     */
    public void cambiarUbicacion(Ubicacion nueva) {
        ubicacion = Objects.requireNonNull(nueva, "nueva");
    }

    /**
     * Habilita un medio de pago en el catálogo del alojamiento (MPAG-06).
     *
     * @throws ReglaDominioException si ya estaba habilitado
     */
    public void habilitarMedioPago(MedioPago medio) {
        Objects.requireNonNull(medio, "medio");
        if (mediosPago.contains(medio)) {
            throw new ReglaDominioException("El medio de pago " + medio.nombre() + " ya está habilitado");
        }
        mediosPago.add(medio);
    }

    /**
     * Deshabilita un medio para pagos nuevos; los pagos históricos que lo usaron no cambian (MPAG-03 · MPAG-06).
     *
     * @throws ReglaDominioException si no estaba habilitado o se quedaría por debajo del mínimo (ALO-06 · MPAG-01)
     */
    public void deshabilitarMedioPago(MedioPago medio) {
        Objects.requireNonNull(medio, "medio");
        if (!mediosPago.contains(medio)) {
            throw new ReglaDominioException("El medio de pago " + medio.nombre() + " no está habilitado");
        }
        validarMinimos(parametros, mediosPago.size() - 1, serviciosActivos());
        mediosPago.remove(medio);
    }

    /**
     * Indica si un pago puede registrarse con este medio: debe estar habilitado en este momento (RN-15 · MPAG-02).
     */
    public boolean aceptaMedioPago(MedioPago medio) {
        return mediosPago.contains(medio);
    }

    /**
     * Agrega un servicio adicional al catálogo (SERV-01).
     *
     * @throws ReglaDominioException si ya existe un servicio con ese id
     */
    public void agregarServicioAdicional(ServicioAdicional servicio) {
        Objects.requireNonNull(servicio, "servicio");
        if (serviciosAdicionales.contains(servicio)) {
            throw new ReglaDominioException("Ya existe el servicio adicional " + servicio.id().valor());
        }
        serviciosAdicionales.add(servicio);
    }

    /**
     * Cambia el precio de un servicio; los cargos ya registrados no cambian (SERV-02).
     *
     * @throws ReglaDominioException si el servicio no existe
     */
    public void cambiarValorServicio(ServicioAdicionalId id, Dinero nuevoValor) {
        servicio(id).cambiarValor(nuevoValor);
    }

    /**
     * Retira un servicio del catálogo sin borrarlo (SERV-04).
     *
     * @throws ReglaDominioException si no existe, ya estaba inactivo o se quedaría por debajo del mínimo de
     *                               servicios activos (ALO-06 · SERV-05)
     */
    public void desactivarServicio(ServicioAdicionalId id) {
        ServicioAdicional servicio = servicio(id);
        if (servicio.activo()) {
            validarMinimos(parametros, mediosPago.size(), serviciosActivos() - 1);
        }
        servicio.desactivar();
    }

    private ServicioAdicional servicio(ServicioAdicionalId id) {
        return serviciosAdicionales.stream()
                .filter(s -> s.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ReglaDominioException("El alojamiento no tiene el servicio " + id.valor()));
    }

    private long serviciosActivos() {
        return serviciosAdicionales.stream().filter(ServicioAdicional::activo).count();
    }

    // ALO-06 · MPAG-01 · SERV-05
    private static void validarMinimos(ParametrosAlojamiento parametros, long medios, long servicios) {
        if (medios < parametros.minimoMediosPago()) {
            throw new ReglaDominioException(
                    "El alojamiento debe tener al menos " + parametros.minimoMediosPago() + " medios de pago");
        }
        if (servicios < parametros.minimoServiciosAdicionales()) {
            throw new ReglaDominioException("El alojamiento debe tener al menos "
                    + parametros.minimoServiciosAdicionales() + " servicios adicionales activos");
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    public AlojamientoId id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }

    public String ciudad() {
        return ciudad;
    }

    public String direccion() {
        return direccion;
    }

    public Ubicacion ubicacion() {
        return ubicacion;
    }

    public String normas() {
        return normas;
    }

    public ParametrosAlojamiento parametros() {
        return parametros;
    }

    public List<ServicioAdicional> serviciosAdicionales() {
        return List.copyOf(serviciosAdicionales);
    }

    public List<MedioPago> mediosPago() {
        return List.copyOf(mediosPago);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Alojamiento otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
