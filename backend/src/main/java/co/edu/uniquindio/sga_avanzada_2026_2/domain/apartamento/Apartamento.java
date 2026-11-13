package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import java.util.stream.IntStream;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Raíz del agregado Apartamento: única unidad reservable; sus dormitorios y áreas no se venden por separado
 * (APA-14). Las reglas que cruzan reservas o tarifas llegan resueltas por parámetro desde los servicios de dominio.
 */
public class Apartamento {

    private static final int MAXIMO_IMAGENES = 10;

    private final ApartamentoId codigo;
    private final AlojamientoId alojamientoId;
    private String nombre;
    private String descripcion;
    private Capacidad capacidad;
    private Dormitorio dormitorios;
    private EstadoOperativo estadoOperativo;
    private List<Imagen> imagenes;               // 0..10; sin imágenes no se puede activar
    private List<Caracteristica> caracteristicas; // ≥1
    private final List<Bloqueo> bloqueos;
    private boolean activo;
    private Integer limiteMascotas;              // vacío: el del alojamiento (RP-03)
    private String motivoFueraDeServicio;        // solo mientras está FUERA_DE_SERVICIO (DEC-113)

    // IMG-01 · IMG-02 · CARAC-01
    public Apartamento(ApartamentoId codigo, AlojamientoId alojamientoId, String nombre, String descripcion,
                       Capacidad capacidad, Dormitorio dormitorios, EstadoOperativo estadoOperativo,
                       List<Imagen> imagenes, List<Caracteristica> caracteristicas, List<Bloqueo> bloqueos,
                       boolean activo) {
        this(codigo, alojamientoId, nombre, descripcion, capacidad, dormitorios, estadoOperativo, imagenes,
                caracteristicas, bloqueos, activo, null);
    }

    /**
     * Reconstrucción con su propio límite de mascotas (RP-03).
     *
     * @param limiteMascotas las que admite este apartamento por reserva; vacío, las del alojamiento
     */
    public Apartamento(ApartamentoId codigo, AlojamientoId alojamientoId, String nombre, String descripcion,
                       Capacidad capacidad, Dormitorio dormitorios, EstadoOperativo estadoOperativo,
                       List<Imagen> imagenes, List<Caracteristica> caracteristicas, List<Bloqueo> bloqueos,
                       boolean activo, Integer limiteMascotas) {
        this(codigo, alojamientoId, nombre, descripcion, capacidad, dormitorios, estadoOperativo, imagenes,
                caracteristicas, bloqueos, activo, limiteMascotas, null);
    }

    /**
     * Reconstrucción completa, con el motivo por el que está fuera de servicio (DEC-113).
     *
     * @param limiteMascotas        las que admite este apartamento por reserva; vacío, las del alojamiento
     * @param motivoFueraDeServicio solo si está FUERA_DE_SERVICIO; vacío si no se dio
     */
    public Apartamento(ApartamentoId codigo, AlojamientoId alojamientoId, String nombre, String descripcion,
                       Capacidad capacidad, Dormitorio dormitorios, EstadoOperativo estadoOperativo,
                       List<Imagen> imagenes, List<Caracteristica> caracteristicas, List<Bloqueo> bloqueos,
                       boolean activo, Integer limiteMascotas, String motivoFueraDeServicio) {
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
        exigirLimiteValido(limiteMascotas);
        this.limiteMascotas = limiteMascotas;
        String motivo = sinBlancos(motivoFueraDeServicio);
        if (motivo != null && estadoOperativo != EstadoOperativo.FUERA_DE_SERVICIO) { // DEC-113
            throw new ReglaDominioException("Solo un apartamento fuera de servicio tiene motivo de fuera de servicio");
        }
        this.motivoFueraDeServicio = motivo;
    }

    /**
     * Crea un apartamento nuevo: nace inactivo (se activa con tarifas completas, APA-11), sin bloqueos y en
     * PENDIENTE_PREPARACION, para que nunca se entregue sin revisar (RN-11 · DEC-34). Las invariantes de imágenes
     * y características las valida el constructor (IMG-01 · IMG-02 · CARAC-01).
     *
     * @throws ReglaDominioException si algún dato viola una invariante del apartamento
     */
    public static Apartamento crear(ApartamentoId codigo, AlojamientoId alojamientoId, String nombre,
                                    String descripcion, Capacidad capacidad, Dormitorio dormitorios,
                                    List<Imagen> imagenes, List<Caracteristica> caracteristicas) {
        return new Apartamento(codigo, alojamientoId, nombre, descripcion, capacidad, dormitorios,
                EstadoOperativo.PENDIENTE_PREPARACION, imagenes, caracteristicas, List.of(), false);
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
     * Corrige los datos descriptivos del apartamento (CU-31 · CARAC-01). La capacidad tiene su propia operación,
     * porque advierte de las reservas afectadas (APA-15).
     *
     * @throws ReglaDominioException si falta el nombre, los dormitorios o no queda ninguna característica
     */
    public void actualizarDatos(String nuevoNombre, String nuevaDescripcion, Dormitorio nuevosDormitorios,
                                List<Caracteristica> nuevasCaracteristicas) {
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new ReglaDominioException("El nombre del apartamento es obligatorio");
        }
        if (nuevosDormitorios == null) {
            throw new ReglaDominioException("Los dormitorios del apartamento son obligatorios");
        }
        if (nuevasCaracteristicas == null || nuevasCaracteristicas.isEmpty()) {
            throw new ReglaDominioException("El apartamento debe declarar al menos una característica");
        }
        nombre = nuevoNombre.trim();
        descripcion = nuevaDescripcion;
        dormitorios = nuevosDormitorios;
        caracteristicas = List.copyOf(nuevasCaracteristicas);
    }

    /**
     * Fija cuántas mascotas admite este apartamento por reserva, por ejemplo 0 si no las recibe aunque el alojamiento
     * sí. Que el alojamiento las acepte y lo que cobra siguen siendo del alojamiento (RP-03 · DEC-93).
     *
     * @param limite vacío para volver al límite del alojamiento
     * @throws ReglaDominioException si el límite es negativo
     */
    public void fijarLimiteMascotas(Integer limite) {
        exigirLimiteValido(limite);
        limiteMascotas = limite;
    }

    private static void exigirLimiteValido(Integer limite) {
        if (limite != null && limite < 0) {
            throw new ReglaDominioException("El límite de mascotas del apartamento no puede ser negativo");
        }
    }

    /**
     * Agrega una imagen; la primera queda como principal (IMG-01 · IMG-02).
     *
     * @throws ReglaDominioException si ya tiene el máximo de imágenes o esa url ya está
     */
    public void agregarImagen(String url) {
        Imagen nueva = new Imagen(url, imagenes.isEmpty());
        if (imagenes.size() >= MAXIMO_IMAGENES) {
            throw new ReglaDominioException("Un apartamento admite máximo " + MAXIMO_IMAGENES + " imágenes");
        }
        if (imagenes.stream().anyMatch(i -> i.url().equals(nueva.url()))) {
            throw new ReglaDominioException("El apartamento ya tiene la imagen " + nueva.url());
        }
        List<Imagen> todas = new ArrayList<>(imagenes);
        todas.add(nueva);
        imagenes = List.copyOf(todas);
    }

    /**
     * Quita una imagen; si era la principal, la primera de las que quedan toma su lugar (IMG-02). Un apartamento a la
     * venta no puede quedarse sin imágenes (IMG-01).
     *
     * @throws ReglaDominioException si la imagen no existe o es la última de un apartamento activo
     */
    public void quitarImagen(String url) {
        exigirImagen(url);
        if (activo && imagenes.size() == 1) {
            throw new ReglaDominioException("Un apartamento a la venta no puede quedarse sin imágenes");
        }
        List<Imagen> restantes = imagenes.stream().filter(i -> !i.url().equals(url.trim())).toList();
        boolean falta = restantes.stream().noneMatch(Imagen::principal);
        imagenes = IntStream.range(0, restantes.size())
                .mapToObj(i -> new Imagen(restantes.get(i).url(), restantes.get(i).principal() || (falta && i == 0)))
                .toList();
    }

    /**
     * Destaca una imagen: exactamente una es la principal (IMG-02).
     *
     * @throws ReglaDominioException si la imagen no existe
     */
    public void marcarImagenPrincipal(String url) {
        exigirImagen(url);
        imagenes = imagenes.stream().map(i -> new Imagen(i.url(), i.url().equals(url.trim()))).toList();
    }

    private void exigirImagen(String url) {
        if (url == null || imagenes.stream().noneMatch(i -> i.url().equals(url.trim()))) {
            throw new ReglaDominioException("El apartamento " + codigo.valor() + " no tiene la imagen " + url);
        }
    }

    /** Lo que el personal de servicio tiene por preparar (CU-41). */
    public boolean pendienteDePreparacion() {
        return estadoOperativo == EstadoOperativo.PENDIENTE_PREPARACION
                || estadoOperativo == EstadoOperativo.EN_PREPARACION;
    }

    /** Los bloqueos vigentes que tocan alguna noche del rango (CU-24 · BLO-03). */
    public List<Bloqueo> bloqueosVigentesEn(Estancia rango) {
        return bloqueos.stream()
                .filter(b -> rango.noches().stream().anyMatch(b::cubre))
                .toList();
    }

    /**
     * El personal empieza a preparar el apartamento (CU-42).
     *
     * @throws ReglaDominioException si no estaba PENDIENTE_PREPARACION (EOPE-02)
     */
    public void iniciarPreparacion() {
        transitarA(EstadoOperativo.EN_PREPARACION);
    }

    /**
     * El apartamento queda listo para entregarse (CU-42 · RN-11).
     *
     * @throws ReglaDominioException si no estaba EN_PREPARACION (EOPE-02)
     */
    public void marcarPreparado() {
        transitarA(EstadoOperativo.PREPARADO);
    }

    /**
     * El apartamento entra a la ruta de preparación: tras la salida de un grupo o al volver de estar fuera de
     * servicio, para que nunca se entregue sin revisar (RN-11 · DEC-21).
     *
     * @throws ReglaDominioException si no estaba OCUPADO ni FUERA_DE_SERVICIO (EOPE-02)
     */
    public void marcarPendientePreparacion() {
        transitarA(EstadoOperativo.PENDIENTE_PREPARACION);
    }

    /**
     * Un grupo recibe el apartamento (CU-21 · RN-11).
     *
     * @throws ReglaDominioException si no estaba PREPARADO (EOPE-02)
     */
    public void ocupar() {
        transitarA(EstadoOperativo.OCUPADO);
    }

    /**
     * Saca el apartamento de operación sin decir por qué (CU-40). Mientras siga así no se vende (DEC-110).
     *
     * @throws ReglaDominioException si está OCUPADO o ya estaba fuera de servicio (EOPE-02)
     */
    public void declararFueraDeServicio() {
        declararFueraDeServicio(null);
    }

    /**
     * Saca el apartamento de operación (CU-40) con el motivo, que recepción y el personal ven mientras siga fuera de
     * servicio (DEC-113). Mientras siga así no se vende (DEC-110).
     *
     * @param motivo opcional; en blanco cuenta como no dado
     * @throws ReglaDominioException si está OCUPADO o ya estaba fuera de servicio (EOPE-02)
     */
    public void declararFueraDeServicio(String motivo) {
        transitarA(EstadoOperativo.FUERA_DE_SERVICIO);
        motivoFueraDeServicio = sinBlancos(motivo);
    }

    // EOPE-02. No crea ni levanta bloqueos (BLO-05)
    private void transitarA(EstadoOperativo destino) {
        if (!estadoOperativo.puedePasarA(destino)) {
            throw new ReglaDominioException(
                    "El apartamento " + codigo.valor() + " está " + estadoOperativo.texto() + " y no puede quedar "
                    + destino.texto());
        }
        estadoOperativo = destino;
        motivoFueraDeServicio = null; // DEC-113: el motivo no sobrevive al salir de fuera de servicio
    }

    private static String sinBlancos(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
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
            throw new ReglaDominioException("Ese bloqueo ya existe");
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
                .orElseThrow(() -> new ReglaDominioException(
                        "El apartamento " + codigo.valor() + " no tiene ese bloqueo"))
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

    /** Las mascotas que admite por reserva, si tiene un límite propio; si no, rige el del alojamiento (RP-03). */
    public Optional<Integer> limiteMascotas() {
        return Optional.ofNullable(limiteMascotas);
    }

    /** Por qué está fuera de servicio, si se dijo; vacío en cualquier otro estado (DEC-113). */
    public Optional<String> motivoFueraDeServicio() {
        return Optional.ofNullable(motivoFueraDeServicio);
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
