package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Extras;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MotivoRechazo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;

import java.time.Duration;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Raíz del agregado Reserva. Nace PENDIENTE; valor, desglose y política se congelan al crear y solo una modificación
 * explícita los recalcula (RN-22). Disponibilidad, tarifas y estancia mínima llegan resueltas por parámetro desde los
 * servicios de dominio.
 */
public class Reserva {

    private final ReservaId codigo;
    private ApartamentoId apartamentoId;
    private TitularId titularId;          // cambia solo con cambiarTitular (TIT-01 · DEC-97)
    private Estancia estancia;
    private EstadoReserva estado;
    private final CanalOrigen canalOrigen;
    private final CanalId canalId;     // solo si EXTERNO (CORI-03)
    private final String idExterno;    // solo si EXTERNO (CORI-03)
    private List<Ocupante> ocupantes;
    private final List<Registro> registros; // historial; a lo sumo uno vigente (REG-06)
    private Salida salida;             // opcional
    private LocalTime horaEstimadaLlegada; // opcional hasta confirmar (RN-09)
    private Dinero valorTotal;               // congelado
    private List<LineaCotizacion> desglose;  // congelado, por noche
    private final PoliticaId politicaVersionId; // congelada
    private final LocalDateTime creadaEn;       // desde aquí corre el plazo de confirmación (RN-21)
    private int carros;                         // RP-02
    private int motos;                          // RP-02
    private int mascotas;                       // RP-03
    private Extras extras;                      // congelados; DEC-89
    private DeclaracionNoShow declaracionNoShow; // nula en los no-show anteriores a BL-25

    // OCU-02 · OCU-05 · CORI-03 · RN-22 · REG-01 · REG-06 · SAL-01
    public Reserva(ReservaId codigo, ApartamentoId apartamentoId, TitularId titularId, Estancia estancia,
                   EstadoReserva estado, CanalOrigen canalOrigen, CanalId canalId, String idExterno,
                   List<Ocupante> ocupantes, List<Registro> registros, Salida salida,
                   LocalTime horaEstimadaLlegada, Dinero valorTotal, List<LineaCotizacion> desglose,
                   PoliticaId politicaVersionId, LocalDateTime creadaEn) {
        this(codigo, apartamentoId, titularId, estancia, estado, canalOrigen, canalId, idExterno, ocupantes, registros,
                salida, horaEstimadaLlegada, valorTotal, desglose, politicaVersionId, creadaEn, 0, 0);
    }

    // Con vehículos y mascotas sin cobro, para lo que no usa esas reglas
    public Reserva(ReservaId codigo, ApartamentoId apartamentoId, TitularId titularId, Estancia estancia,
                   EstadoReserva estado, CanalOrigen canalOrigen, CanalId canalId, String idExterno,
                   List<Ocupante> ocupantes, List<Registro> registros, Salida salida,
                   LocalTime horaEstimadaLlegada, Dinero valorTotal, List<LineaCotizacion> desglose,
                   PoliticaId politicaVersionId, LocalDateTime creadaEn, int vehiculos, int mascotas) {
        this(codigo, apartamentoId, titularId, estancia, estado, canalOrigen, canalId, idExterno, ocupantes, registros,
                salida, horaEstimadaLlegada, valorTotal, desglose, politicaVersionId, creadaEn, vehiculos, 0, mascotas,
                Extras.NINGUNO);
    }

    // Reconstrucción con lo que la reserva declaró de vehículos y mascotas y lo que cuestan (RP-02 · RP-03 · DEC-89)
    public Reserva(ReservaId codigo, ApartamentoId apartamentoId, TitularId titularId, Estancia estancia,
                   EstadoReserva estado, CanalOrigen canalOrigen, CanalId canalId, String idExterno,
                   List<Ocupante> ocupantes, List<Registro> registros, Salida salida,
                   LocalTime horaEstimadaLlegada, Dinero valorTotal, List<LineaCotizacion> desglose,
                   PoliticaId politicaVersionId, LocalDateTime creadaEn, int carros, int motos, int mascotas,
                   Extras extras) {
        this(codigo, apartamentoId, titularId, estancia, estado, canalOrigen, canalId, idExterno, ocupantes, registros,
                salida, horaEstimadaLlegada, valorTotal, desglose, politicaVersionId, creadaEn, carros, motos, mascotas,
                extras, null);
    }

    // Reconstrucción completa: también quién declaró el no-show, que solo existe en ese estado (BL-25 · DEC-103)
    public Reserva(ReservaId codigo, ApartamentoId apartamentoId, TitularId titularId, Estancia estancia,
                   EstadoReserva estado, CanalOrigen canalOrigen, CanalId canalId, String idExterno,
                   List<Ocupante> ocupantes, List<Registro> registros, Salida salida,
                   LocalTime horaEstimadaLlegada, Dinero valorTotal, List<LineaCotizacion> desglose,
                   PoliticaId politicaVersionId, LocalDateTime creadaEn, int carros, int motos, int mascotas,
                   Extras extras, DeclaracionNoShow declaracionNoShow) {
        if (codigo == null || apartamentoId == null || titularId == null || estancia == null || estado == null
                || canalOrigen == null || ocupantes == null || registros == null || valorTotal == null
                || desglose == null || politicaVersionId == null || creadaEn == null) {
            throw new ReglaDominioException("Faltan datos obligatorios de la reserva");
        }
        validarOrigen(canalOrigen, canalId, idExterno);
        validarGrupo(ocupantes);
        new Cotizacion(desglose, valorTotal); // el total congelado es la suma del desglose (RN-05)
        boolean inicio = estado == EstadoReserva.EN_CURSO || estado == EstadoReserva.FINALIZADA;
        long vigentes = registros.stream().filter(r -> !r.anulado()).count();
        if (vigentes != (inicio ? 1 : 0) || (!inicio && !registros.isEmpty())
                || (estado == EstadoReserva.FINALIZADA) != (salida != null)) {
            throw new ReglaDominioException("El registro y la salida no corresponden al estado " + estado);
        }
        if (declaracionNoShow != null && estado != EstadoReserva.NO_SHOW) {
            throw new ReglaDominioException("La declaración del no-show no corresponde al estado " + estado);
        }
        this.declaracionNoShow = declaracionNoShow;
        this.codigo = codigo;
        this.apartamentoId = apartamentoId;
        this.titularId = titularId;
        this.estancia = estancia;
        this.estado = estado;
        this.canalOrigen = canalOrigen;
        this.canalId = canalId;
        this.idExterno = idExterno;
        this.ocupantes = List.copyOf(ocupantes);
        this.registros = new ArrayList<>(registros);
        this.salida = salida;
        this.horaEstimadaLlegada = horaEstimadaLlegada;
        this.valorTotal = valorTotal;
        this.desglose = List.copyOf(desglose);
        this.politicaVersionId = politicaVersionId;
        this.creadaEn = creadaEn;
        validarDeclarados(carros, motos, mascotas, extras);
        this.carros = carros;
        this.motos = motos;
        this.mascotas = mascotas;
        this.extras = extras;
    }

    /**
     * Anota los vehículos y mascotas de la reserva y congela lo que cuestan (DEC-89). Que quepan en el parqueadero
     * (RP-02) lo verifica antes {@code CupoParqueaderoDomainService}, y que se acepten (RP-03),
     * {@code ParametrosAlojamiento.exigirMascotas}.
     *
     * @param nuevosExtras de {@code ParametrosAlojamiento.calcularExtras}
     * @throws ReglaDominioException si alguna cantidad es negativa o faltan los extras
     */
    public void declararExtras(int nuevosCarros, int nuevasMotos, int nuevasMascotas, Extras nuevosExtras) {
        validarDeclarados(nuevosCarros, nuevasMotos, nuevasMascotas, nuevosExtras);
        carros = nuevosCarros;
        motos = nuevasMotos;
        mascotas = nuevasMascotas;
        extras = nuevosExtras;
    }

    private static void validarDeclarados(int carros, int motos, int mascotas, Extras extras) {
        if (carros < 0 || motos < 0 || mascotas < 0) {
            throw new ReglaDominioException("Los vehículos y las mascotas de la reserva no pueden ser negativos");
        }
        if (extras == null) {
            throw new ReglaDominioException("Faltan los extras de la reserva");
        }
    }

    public int carros() {
        return carros;
    }

    public int motos() {
        return motos;
    }

    /** Carros y motos ocupan el mismo cupo (RP-02). */
    public int vehiculos() {
        return carros + motos;
    }

    public Extras extras() {
        return extras;
    }

    /** Quién declaró el no-show y cuándo; nula si no lo es o si se declaró antes de guardarse el dato (BL-25). */
    public DeclaracionNoShow declaracionNoShow() {
        return declaracionNoShow;
    }

    /** La estancia y sus extras: la base del anticipo (RES-15 · DEC-89). */
    public Dinero valorConExtras() {
        return valorTotal.sumar(extras.total());
    }

    /** Lo que debe estar pagado para confirmar: ese porcentaje de la estancia y sus extras (RES-15 · DEC-89). */
    public Dinero anticipoExigido(Porcentaje anticipo) {
        return valorConExtras().porcentaje(anticipo);
    }

    /**
     * La parte de lo pagado que corresponde a la estancia, repartida en proporción a su valor: la retención por
     * cancelación o no-show no alcanza a los extras (DEC-89).
     *
     * @param pagado total pagado en el folio de la reserva
     */
    public Dinero pagadoPorLaEstancia(Dinero pagado) {
        if (!extras.total().esPositivo()) {
            return pagado;
        }
        return new Dinero(pagado.monto().multiply(valorTotal.monto())
                .divide(valorConExtras().monto(), 0, RoundingMode.HALF_UP));
    }

    public int mascotas() {
        return mascotas;
    }

    /**
     * Crea una reserva PENDIENTE y congela su valor, desglose y versión de política (RN-22 · RES-22). Que la
     * estancia sea válida (RN-03) lo garantiza {@link Estancia}; que el apartamento esté activo, sin solapes, sin
     * bloqueos y con tiempo de preparación (RN-01 · RN-07 · RN-20) lo verifica antes
     * {@code DisponibilidadDomainService}.
     *
     * @param cotizacion           de {@code TarificacionDomainService} para esta estancia y estos ocupantes
     * @param capacidad            capacidad actual del apartamento (RN-02)
     * @param titular              responsable de la reserva; puede no alojarse (TIT-01 · DEC-97)
     * @param estanciaMinimaNoches de {@code CalendarioTemporadas.estanciaMinimaPara} (RP-01)
     * @param ahora                fecha y hora actuales en Colombia, inyectadas (EST-06)
     * @throws ReglaDominioException si la entrada es anterior a hoy (RN-04), el grupo excede la capacidad (RN-02),
     *                               no hay ocupantes (OCU-02), hay uno repetido (OCU-05) o nacido en el futuro
     *                               (OCU-12), el huésped principal sobra, falta o es menor de edad (DEC-97), no se
     *                               cumple la
     *                               estancia mínima (RP-01), el canal externo no trae su referencia (CORI-03) o la
     *                               cotización no corresponde a la estancia
     */
    public static Reserva crear(ReservaId codigo, ApartamentoId apartamentoId, Titular titular,
                                Estancia estancia, CanalOrigen canalOrigen, CanalId canalId, String idExterno,
                                List<Ocupante> ocupantes, LocalTime horaEstimadaLlegada, Cotizacion cotizacion,
                                PoliticaId politicaVersionId, Capacidad capacidad, int estanciaMinimaNoches,
                                LocalDateTime ahora) {
        Objects.requireNonNull(ahora, "ahora");
        Objects.requireNonNull(titular, "titular");
        validarCondiciones(estancia, ocupantes, cotizacion, capacidad, estanciaMinimaNoches, ahora.toLocalDate());
        List<Ocupante> grupo = conHuespedPrincipal(ocupantes, estancia.entrada());
        return new Reserva(codigo, apartamentoId, titular.id(), estancia, EstadoReserva.PENDIENTE, canalOrigen,
                canalId, idExterno, grupo, List.of(), null, horaEstimadaLlegada, cotizacion.total(),
                cotizacion.desglose(), politicaVersionId, ahora);
    }

    /**
     * Cambia fechas, ocupantes o apartamento de una reserva que no ha iniciado. Revalida las condiciones de creación
     * y reemplaza el valor congelado por la nueva cotización; la política congelada no cambia (RN-14 · RES-14 ·
     * OCU-06). La diferencia de valor la registra el caso de uso como ajuste en el folio (A5); la disponibilidad
     * sin contar esta misma reserva la verifica antes {@code DisponibilidadDomainService}.
     *
     * @param cotizacion           con las tarifas vigentes al momento de la modificación
     * @param capacidad            capacidad actual del apartamento destino (RN-02)
     * @param estanciaMinimaNoches de {@code CalendarioTemporadas.estanciaMinimaPara} para la nueva estancia (RP-01)
     * @param titular              el titular de esta reserva; puede no estar entre los ocupantes (DEC-97)
     * @param hoy                  fecha actual en Colombia, inyectada
     * @throws ReglaDominioException si la reserva no está PENDIENTE ni CONFIRMADA, el titular no es el de la reserva,
     *                               o los nuevos datos violan alguna condición de creación (RN-04 · RN-02 · OCU-02 ·
     *                               OCU-05 · OCU-12 · RP-01)
     */
    public void modificar(Estancia nuevaEstancia, List<Ocupante> nuevosOcupantes, ApartamentoId nuevoApartamentoId,
                          Cotizacion cotizacion, Capacidad capacidad, int estanciaMinimaNoches, Titular titular,
                          LocalDate hoy) {
        if (estado != EstadoReserva.PENDIENTE && estado != EstadoReserva.CONFIRMADA) {
            throw new ReglaDominioException(MotivoRechazo.ESTADO_NO_PERMITIDO,
                    "Solo se modifica una reserva PENDIENTE o CONFIRMADA; está " + estado);
        }
        Objects.requireNonNull(nuevoApartamentoId, "nuevoApartamentoId");
        Objects.requireNonNull(hoy, "hoy");
        Objects.requireNonNull(titular, "titular");
        if (!titular.id().equals(titularId)) {
            throw new ReglaDominioException("El titular " + titular.id().valor() + " no es el de la reserva");
        }
        validarCondiciones(nuevaEstancia, nuevosOcupantes, cotizacion, capacidad, estanciaMinimaNoches, hoy);
        List<Ocupante> grupo = conHuespedPrincipal(nuevosOcupantes, nuevaEstancia.entrada());
        estancia = nuevaEstancia;
        ocupantes = grupo.stream().map(this::conservandoDatosTra).toList();
        apartamentoId = nuevoApartamentoId;
        valorTotal = cotizacion.total();
        desglose = cotizacion.desglose();
    }

    /**
     * Registra o corrige la hora estimada de llegada, requisito para confirmar (RN-09).
     *
     * @throws ReglaDominioException si la reserva ya inició o terminó
     */
    public void indicarHoraEstimadaLlegada(LocalTime hora) {
        Objects.requireNonNull(hora, "hora");
        if (estado != EstadoReserva.PENDIENTE && estado != EstadoReserva.CONFIRMADA) {
            throw new ReglaDominioException("La hora estimada de llegada solo se indica antes de iniciar la estancia");
        }
        horaEstimadaLlegada = hora;
    }

    /**
     * Pasa de PENDIENTE a CONFIRMADA (RN-08). Exige la hora estimada de llegada (RN-09) y, si el alojamiento pide
     * anticipo, que los pagos cubran ese porcentaje del valor congelado con sus extras (RES-15 · DEC-89).
     *
     * Tampoco se confirma una reserva cuya hora límite de llegada ya pasó (la del no-show, RES-16): confirmarla solo
     * serviría para declararla no-show enseguida. Se exceptúa la que se creó después de esa hora, como la de quien
     * llega tarde al mostrador ese mismo día (pruebas del sprint 3).
     *
     * @param anticipo   {@code ParametrosAlojamiento.anticipo}; 0 = no se exige
     * @param pagado     total pagado en el folio de la reserva
     * @param ahora      fecha y hora actuales en Colombia, inyectadas
     * @param horaLimite {@code ParametrosAlojamiento.horaLimiteNoShow}
     * @throws ReglaDominioException si no está PENDIENTE (RN-08), ya pasó la hora límite de llegada, falta la hora
     *                               estimada (RN-09) o el pago no cubre el anticipo (RES-15)
     */
    public void confirmar(Porcentaje anticipo, Dinero pagado, LocalDateTime ahora, LocalTime horaLimite) {
        Objects.requireNonNull(anticipo, "anticipo");
        Objects.requireNonNull(pagado, "pagado");
        Objects.requireNonNull(ahora, "ahora");
        Objects.requireNonNull(horaLimite, "horaLimite");
        validarTransicion(EstadoReserva.CONFIRMADA);
        if (llegadaVencidaParaConfirmar(ahora, horaLimite)) {
            throw new ReglaDominioException(MotivoRechazo.LLEGADA_VENCIDA,
                    "Ya pasó la hora límite de llegada (" + estancia.entrada().atTime(horaLimite)
                    + "): la reserva no se confirma; se cancela o se hace una nueva");
        }
        if (horaEstimadaLlegada == null) {
            throw new ReglaDominioException(MotivoRechazo.FALTA_HORA_LLEGADA,
                    "La reserva necesita hora estimada de llegada para confirmarse");
        }
        Dinero exigido = anticipoExigido(anticipo);
        if (pagado.esMenorQue(exigido)) {
            throw new ReglaDominioException(MotivoRechazo.ANTICIPO_INSUFICIENTE,
                    "El pago (" + pagado.monto() + ") no cubre el anticipo exigido ("
                    + exigido.monto() + ")");
        }
        estado = EstadoReserva.CONFIRMADA;
    }

    // DEC-110 · #20: pasó la hora límite de llegada y la reserva ya existía antes de esa hora
    private boolean llegadaVencidaParaConfirmar(LocalDateTime ahora, LocalTime horaLimite) {
        LocalDateTime limite = estancia.entrada().atTime(horaLimite);
        return ahora.isAfter(limite) && creadaEn.isBefore(limite);
    }

    /**
     * CU-41 · DEC-111: indica si todavía se espera a este huésped, para ordenar la preparación de los apartamentos.
     * Se espera una reserva CONFIRMADA que entra hoy o después (aunque hoy ya haya pasado la hora límite: mientras no
     * se declare el no-show, puede llegar) y una PENDIENTE que entra hoy o después y aún se puede confirmar (DEC-110).
     * No se espera una EN_CURSO (ya llegó), una terminada ni una cuya entrada ya pasó.
     *
     * @param ahora      fecha y hora actuales en Colombia, inyectadas
     * @param horaLimite {@code ParametrosAlojamiento.horaLimiteNoShow}
     */
    public boolean llegadaEsperada(LocalDateTime ahora, LocalTime horaLimite) {
        Objects.requireNonNull(ahora, "ahora");
        Objects.requireNonNull(horaLimite, "horaLimite");
        if (estancia.entrada().isBefore(ahora.toLocalDate())) {
            return false;
        }
        return estado == EstadoReserva.CONFIRMADA
                || (estado == EstadoReserva.PENDIENTE && !llegadaVencidaParaConfirmar(ahora, horaLimite));
    }

    /**
     * Cancela una reserva que no ha iniciado y libera sus noches de inmediato (RN-08 · RN-12). La retención según la
     * política congelada la calcula {@code CancelacionDomainService} (RN-13, A5).
     *
     * @throws ReglaDominioException si no está PENDIENTE ni CONFIRMADA (RN-08)
     */
    public void cancelar() {
        exigirCancelable();
        estado = EstadoReserva.CANCELADA;
    }

    /**
     * Quien reservó en el portal se arrepiente antes de pagar: la reserva se cancela y libera sus noches de inmediato,
     * sin esperar a que venza (RN-12 · DEC-93). Que no tenga pagos lo revisa {@code CancelacionDomainService}.
     *
     * @throws ReglaDominioException si no está PENDIENTE
     */
    public void liberar() {
        if (estado != EstadoReserva.PENDIENTE) {
            throw new ReglaDominioException("Solo se libera una reserva PENDIENTE; está " + estado);
        }
        estado = EstadoReserva.CANCELADA;
    }

    /**
     * RN-08: revisa que la reserva todavía se pueda cancelar, sin cambiarla (DEC-91).
     *
     * @throws ReglaDominioException si no está PENDIENTE ni CONFIRMADA
     */
    public void exigirCancelable() {
        validarTransicion(EstadoReserva.CANCELADA);
    }

    /**
     * Declara que el titular no se presentó y libera las noches (RN-08 · RN-12). Solo a partir de la hora límite
     * del día de entrada (RES-16). Queda quién lo declaró y cuándo (BL-25 · DEC-103).
     *
     * @param ahora      fecha y hora actuales en Colombia, inyectadas
     * @param horaLimite {@code ParametrosAlojamiento.horaLimiteNoShow}
     * @param autor      quien lo declara
     * @throws ReglaDominioException si no está CONFIRMADA (RN-08), aún no llega la hora límite (RES-16) o falta el
     *                               autor
     */
    public void declararNoShow(LocalDateTime ahora, LocalTime horaLimite, UsuarioId autor) {
        exigirNoShowDeclarable(ahora, horaLimite);
        declaracionNoShow = new DeclaracionNoShow(autor, ahora);
        estado = EstadoReserva.NO_SHOW;
    }

    /**
     * RN-08 · RES-16: revisa que ya se pueda declarar el no-show, sin cambiar la reserva (DEC-102).
     *
     * @param ahora      fecha y hora actuales en Colombia, inyectadas
     * @param horaLimite {@code ParametrosAlojamiento.horaLimiteNoShow}
     * @throws ReglaDominioException si no está CONFIRMADA (RN-08) o aún no llega la hora límite (RES-16)
     */
    public void exigirNoShowDeclarable(LocalDateTime ahora, LocalTime horaLimite) {
        Objects.requireNonNull(ahora, "ahora");
        Objects.requireNonNull(horaLimite, "horaLimite");
        validarTransicion(EstadoReserva.NO_SHOW);
        LocalDateTime limite = estancia.entrada().atTime(horaLimite);
        if (ahora.isBefore(limite)) {
            throw new ReglaDominioException(MotivoRechazo.ANTES_DE_TIEMPO,
                    "El no-show solo puede declararse desde " + limite);
        }
    }

    /**
     * Cancela automáticamente una reserva PENDIENTE que superó el plazo de confirmación contado desde su creación
     * (RN-21 · RES-17 · RES-21). Lo invoca el caso de uso {@code VencerReservaUseCase}.
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @param plazo {@code ParametrosAlojamiento.plazoParaConfirmar} según su canal (DEC-69)
     * @throws ReglaDominioException si no está PENDIENTE o todavía no supera el plazo
     */
    public void expirar(LocalDateTime ahora, Duration plazo) {
        Objects.requireNonNull(ahora, "ahora");
        Objects.requireNonNull(plazo, "plazo");
        if (estado != EstadoReserva.PENDIENTE) {
            throw new ReglaDominioException("Solo vence una reserva PENDIENTE; está " + estado);
        }
        if (!vencio(ahora, plazo)) {
            throw new ReglaDominioException("La reserva " + codigo.valor()
                    + " no ha superado el plazo de confirmación");
        }
        estado = EstadoReserva.CANCELADA;
    }

    /**
     * Si es una reserva PENDIENTE que ya superó su plazo para confirmarse (RN-21), aunque todavía nadie la haya
     * vencido.
     */
    public boolean vencio(LocalDateTime ahora, Duration plazo) {
        return estado == EstadoReserva.PENDIENTE && ahora.isAfter(creadaEn.plus(plazo));
    }

    /**
     * Hasta cuándo puede confirmarse, p. ej. para pagarla en línea (DEC-69).
     */
    public LocalDateTime venceEn(Duration plazo) {
        return creadaEn.plus(plazo);
    }

    /**
     * Registra la llegada (check-in) con la fecha, hora y autor reales (REG-05) y pasa a EN_CURSO (RN-08). Que el
     * apartamento esté PREPARADO y pase a OCUPADO en la misma transacción lo coordina
     * {@code RegistroLlegadaDomainService} (RN-11 · REG-04 · D-02).
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @param pagado        lo pagado hasta ahora en el folio (BL-22)
     * @param motivoSinPago por qué entra sin haber pagado la estancia y sus extras; se ignora si ya están pagados
     * @throws ReglaDominioException si no está CONFIRMADA (RN-10 · REG-01) o aún no es la fecha de entrada
     *                               (RN-10 · REG-02)
     */
    public void registrarLlegada(RegistroId id, LocalDateTime ahora, UsuarioId autor, Dinero pagado,
                                 String motivoSinPago) {
        Objects.requireNonNull(ahora, "ahora");
        Objects.requireNonNull(pagado, "pagado");
        if (estado != EstadoReserva.CONFIRMADA) {
            throw new ReglaDominioException(MotivoRechazo.ESTADO_NO_PERMITIDO,
                    "Solo se registra la llegada de una reserva CONFIRMADA; está " + estado);
        }
        if (ahora.toLocalDate().isBefore(estancia.entrada())) {
            throw new ReglaDominioException(MotivoRechazo.ANTES_DE_TIEMPO,
                    "No se puede registrar la llegada antes de la fecha de entrada "
                    + estancia.entrada());
        }
        boolean pagada = !pagado.esMenorQue(valorConExtras());
        if (!pagada && (motivoSinPago == null || motivoSinPago.isBlank())) { // BL-22
            throw new ReglaDominioException(MotivoRechazo.ESTANCIA_SIN_PAGAR,
                    "La estancia y sus extras valen " + valorConExtras().monto() + " y hay pagado " + pagado.monto()
                    + ": cóbrela o registre por qué entra sin pagar");
        }
        registros.add(new Registro(id, ahora, autor, false, null, pagada ? null : motivoSinPago));
        estado = EstadoReserva.EN_CURSO;
    }

    /**
     * Corrige un registro de llegada equivocado: el vigente queda anulado como historial y se agrega uno nuevo con la
     * fecha, hora y autor correctos. La reserva sigue EN_CURSO: no existe transición hacia atrás (REG-06 · RN-08 ·
     * DEC-45).
     *
     * @param fechaHoraCorrecta cuándo llegó realmente el grupo
     * @param ahora             fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si la reserva no está EN_CURSO, falta el motivo, el id ya existe, o la fecha
     *                               corregida es anterior a la entrada (REG-02) o posterior a ahora
     */
    public void corregirRegistro(RegistroId nuevoId, LocalDateTime fechaHoraCorrecta, UsuarioId autor, String motivo,
                                 LocalDateTime ahora) {
        Objects.requireNonNull(fechaHoraCorrecta, "fechaHoraCorrecta");
        Objects.requireNonNull(ahora, "ahora");
        if (estado != EstadoReserva.EN_CURSO) {
            throw new ReglaDominioException(MotivoRechazo.ESTADO_NO_PERMITIDO,
                    "Solo se corrige el registro de una reserva EN_CURSO; está " + estado);
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("La corrección del registro requiere un motivo");
        }
        if (registros.stream().anyMatch(r -> r.id().equals(nuevoId))) {
            throw new ReglaDominioException("Ya existe el registro " + nuevoId.valor());
        }
        if (fechaHoraCorrecta.toLocalDate().isBefore(estancia.entrada())) {
            throw new ReglaDominioException("La llegada corregida no puede ser anterior a la fecha de entrada "
                    + estancia.entrada());
        }
        if (fechaHoraCorrecta.isAfter(ahora)) {
            throw new ReglaDominioException("La llegada corregida no puede ser posterior a este momento");
        }
        Registro corregido = new Registro(nuevoId, fechaHoraCorrecta, autor, false, motivo,
                registro().motivoSinPago());
        registro().anular();
        registros.add(corregido);
    }

    // BL-07: la misma persona que ya estaba en el grupo conserva los datos del TRA que se le habían tomado
    private Ocupante conservandoDatosTra(Ocupante nuevo) {
        return ocupantes.stream()
                .filter(nuevo::esDuplicadoDe)
                .findFirst()
                .map(nuevo::conservandoDatosDe)
                .orElse(nuevo);
    }

    /**
     * Cambia el titular cuando quien reservó no debe serlo, por ejemplo en el registro de llegada (TIT-01 · DEC-97).
     * El titular responde por la reserva y su folio; no tiene que alojarse.
     *
     * @throws ReglaDominioException si la reserva ya terminó o el nuevo titular ya es el de la reserva
     */
    public void cambiarTitular(Titular nuevo) {
        Objects.requireNonNull(nuevo, "nuevo");
        if (!estaActiva()) {
            throw new ReglaDominioException(MotivoRechazo.ESTADO_NO_PERMITIDO,
                    "El titular solo se cambia en una reserva activa; está " + estado);
        }
        if (nuevo.id().equals(titularId)) {
            throw new ReglaDominioException(MotivoRechazo.TITULAR_INVALIDO,
                    nuevo.nombre() + " ya es el titular de la reserva " + codigo.valor());
        }
        titularId = nuevo.id();
    }

    /**
     * Designa otro ocupante como huésped principal del TRA, por ejemplo en el registro de llegada (DEC-97). El anterior
     * pasa a ser acompañante.
     *
     * @throws ReglaDominioException si la reserva ya terminó, el ocupante no es de esta reserva, ya es el principal o
     *                               no es mayor de edad a la fecha de entrada
     */
    public void designarHuespedPrincipal(OcupanteId ocupanteId) {
        Objects.requireNonNull(ocupanteId, "ocupanteId");
        if (!estaActiva()) {
            throw new ReglaDominioException(MotivoRechazo.ESTADO_NO_PERMITIDO,
                    "El huésped principal solo se cambia en una reserva activa; está " + estado);
        }
        Ocupante nuevo = ocupante(ocupanteId);
        if (nuevo.principal()) {
            throw new ReglaDominioException(MotivoRechazo.HUESPED_PRINCIPAL_INVALIDO,
                    nuevo.nombre() + " ya es el huésped principal de la reserva " + codigo.valor());
        }
        exigirMayorDeEdad(nuevo, estancia.entrada());
        ocupantes = ocupantes.stream().map(o -> o.comoPrincipal(o.equals(nuevo))).toList();
    }

    /**
     * Completa los datos del TRA de un ocupante, en la llegada o en cualquier momento antes de la salida (BL-07). El
     * dato que no llega se conserva, y la llegada se puede registrar aunque falten: la salida es la que los exige.
     *
     * @throws ReglaDominioException si la reserva ya terminó, el ocupante no es de esta reserva, llega un documento
     *                               distinto del registrado o el ocupante queda repetido en el grupo (OCU-05)
     */
    public void completarDatosOcupante(OcupanteId ocupanteId, String nombres, String primerApellido,
                                       String segundoApellido, Documento documento, String nacionalidad,
                                       DatosViaje viaje) {
        Objects.requireNonNull(ocupanteId, "ocupanteId");
        if (!estaActiva()) {
            throw new ReglaDominioException(MotivoRechazo.ESTADO_NO_PERMITIDO,
                    "Los datos de los ocupantes solo se completan en una reserva activa; está " + estado);
        }
        Ocupante actual = ocupante(ocupanteId);
        Ocupante completo = actual.completar(nombres, primerApellido, segundoApellido, documento, nacionalidad, viaje);
        List<Ocupante> grupo = ocupantes.stream().map(o -> o.equals(actual) ? completo : o).toList();
        validarGrupo(grupo);
        ocupantes = grupo;
    }

    /**
     * Registra la salida (check-out) y pasa a FINALIZADA, liberando las noches restantes (RN-08 · RN-12 · SAL-06).
     * Una salida anticipada no es cancelación (SAL-04). El folio cerrado (SAL-02) y el apartamento a
     * PENDIENTE_PREPARACION (SAL-03) los coordina {@code SalidaOperativaDomainService}.
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si no está EN_CURSO (SAL-01) o la hora es anterior a la del registro (SAL-05)
     */
    public void registrarSalida(SalidaId id, LocalDateTime ahora, UsuarioId autor) {
        validarSalida(ahora);
        salida = new Salida(id, ahora, autor);
        estado = EstadoReserva.FINALIZADA;
    }

    /**
     * Verifica, sin cambiar nada, que la salida se puede registrar en este momento. Permite a
     * {@code SalidaOperativaDomainService} rechazar antes de cerrar el folio (D-02).
     *
     * @throws ReglaDominioException si no está EN_CURSO (SAL-01), la hora es anterior a la del registro (SAL-05) o
     *                               a algún ocupante le faltan datos del TRA (BL-07)
     */
    public void validarSalida(LocalDateTime ahora) {
        Objects.requireNonNull(ahora, "ahora");
        if (estado != EstadoReserva.EN_CURSO) {
            throw new ReglaDominioException(MotivoRechazo.ESTADO_NO_PERMITIDO,
                    "Solo se registra la salida de una reserva EN_CURSO; está " + estado);
        }
        if (ahora.isBefore(registro().fechaHora())) {
            throw new ReglaDominioException("La salida no puede ser anterior al registro de llegada");
        }
        exigirDatosTra();
    }

    // BL-07: el TRA se reporta con los datos de cada ocupante, así que la estancia no termina sin ellos
    private void exigirDatosTra() {
        String faltantes = ocupantes.stream()
                .filter(o -> !o.datosTraFaltantes().isEmpty())
                .map(o -> o.nombre() + " (" + o.datosTraFaltantes().stream().map(DatoTra::texto)
                        .collect(Collectors.joining(", ")) + ")")
                .collect(Collectors.joining("; "));
        if (!faltantes.isEmpty()) {
            throw new ReglaDominioException(MotivoRechazo.DATOS_TRA_INCOMPLETOS,
                    "Antes de la salida faltan datos del TRA: " + faltantes);
        }
    }

    /**
     * Activa = PENDIENTE, CONFIRMADA o EN_CURSO: retiene sus noches (EDO-03 · RN-12).
     */
    public boolean estaActiva() {
        return estado.retieneDisponibilidad();
    }

    /**
     * Indica si esta reserva impide vender alguna noche de la estancia dada: está activa y se solapa (RN-01 ·
     * EST-03). El canal no cambia la regla (CORI-02).
     */
    public boolean retieneNochesDe(Estancia otra) {
        return estaActiva() && estancia.seSolapaCon(otra);
    }

    private void validarTransicion(EstadoReserva destino) {
        if (!estado.puedePasarA(destino)) {
            throw new ReglaDominioException(MotivoRechazo.ESTADO_NO_PERMITIDO,
                    "Transición de reserva no permitida: " + estado + " → " + destino);
        }
    }

    // Condiciones de crear y de modificar (RN-14): RN-04 · RN-02 · OCU-02 · OCU-05 · OCU-12 · RP-01
    private static void validarCondiciones(Estancia estancia, List<Ocupante> ocupantes, Cotizacion cotizacion,
                                           Capacidad capacidad, int estanciaMinimaNoches, LocalDate hoy) {
        Objects.requireNonNull(estancia, "estancia");
        Objects.requireNonNull(cotizacion, "cotizacion");
        Objects.requireNonNull(capacidad, "capacidad");
        if (estancia.entrada().isBefore(hoy)) {
            throw new ReglaDominioException(MotivoRechazo.FECHAS_INVALIDAS,
                    "La fecha de entrada no puede ser anterior a hoy (" + hoy + ")");
        }
        validarGrupo(ocupantes);
        if (!capacidad.admite(ocupantes.size())) {
            throw new ReglaDominioException(MotivoRechazo.CAPACIDAD_EXCEDIDA, "El grupo de " + ocupantes.size()
                    + " ocupantes excede la capacidad del apartamento (" + capacidad.valor() + ")");
        }
        ocupantes.stream()
                .filter(o -> o.fechaNacimiento().isAfter(hoy))
                .findFirst()
                .ifPresent(o -> {
                    throw new ReglaDominioException("La fecha de nacimiento de " + o.nombre() + " es futura");
                });
        if (estancia.cantidadNoches() < estanciaMinimaNoches) {
            throw new ReglaDominioException(MotivoRechazo.ESTANCIA_MINIMA,
                    "La estancia debe ser de al menos " + estanciaMinimaNoches
                    + " noches en esta temporada");
        }
        if (!cotizacion.desglose().stream().map(LineaCotizacion::noche).toList().equals(estancia.noches())) {
            throw new ReglaDominioException("La cotización no corresponde a las noches de la estancia");
        }
    }

    private Ocupante ocupante(OcupanteId ocupanteId) {
        return ocupantes.stream()
                .filter(o -> o.id().equals(ocupanteId))
                .findFirst()
                .orElseThrow(() -> new ReglaDominioException(
                        "El ocupante " + ocupanteId.valor() + " no es de la reserva " + codigo.valor()));
    }

    // DEC-97 · Resolución 700 de 2021: un solo huésped principal, que se aloja y es mayor de edad. Si nadie viene
    // marcado, lo es el primer mayor de edad del grupo
    private static List<Ocupante> conHuespedPrincipal(List<Ocupante> grupo, LocalDate entrada) {
        List<Ocupante> marcados = grupo.stream().filter(Ocupante::principal).toList();
        if (marcados.size() > 1) {
            throw new ReglaDominioException(MotivoRechazo.HUESPED_PRINCIPAL_INVALIDO,
                    "Solo un ocupante puede ser el huésped principal");
        }
        Ocupante principal = marcados.isEmpty()
                ? grupo.stream().filter(o -> o.esMayorDeEdadA(entrada)).findFirst()
                        .orElseThrow(() -> new ReglaDominioException(MotivoRechazo.HUESPED_PRINCIPAL_INVALIDO,
                                "El grupo debe traer un mayor de edad que sea el huésped principal"))
                : marcados.getFirst();
        exigirMayorDeEdad(principal, entrada);
        return grupo.stream().map(o -> o.comoPrincipal(o.equals(principal))).toList();
    }

    private static void exigirMayorDeEdad(Ocupante principal, LocalDate entrada) {
        if (!principal.esMayorDeEdadA(entrada)) {
            throw new ReglaDominioException(MotivoRechazo.HUESPED_PRINCIPAL_INVALIDO, "El huésped principal, "
                    + principal.nombre() + ", debe ser mayor de edad (" + Ocupante.MAYORIA_DE_EDAD + " años)");
        }
    }

    // OCU-02 · OCU-05
    private static void validarGrupo(List<Ocupante> ocupantes) {
        Objects.requireNonNull(ocupantes, "ocupantes");
        if (ocupantes.isEmpty()) {
            throw new ReglaDominioException("La reserva debe tener al menos un ocupante");
        }
        for (int i = 0; i < ocupantes.size(); i++) {
            for (int j = i + 1; j < ocupantes.size(); j++) {
                if (ocupantes.get(i).esDuplicadoDe(ocupantes.get(j))) {
                    throw new ReglaDominioException("El ocupante " + ocupantes.get(j).nombre() + " está repetido");
                }
            }
        }
    }

    // CORI-03
    private static void validarOrigen(CanalOrigen canalOrigen, CanalId canalId, String idExterno) {
        boolean tieneReferencia = canalId != null && idExterno != null && !idExterno.isBlank();
        boolean sinReferencia = canalId == null && idExterno == null;
        if (canalOrigen == CanalOrigen.EXTERNO ? !tieneReferencia : !sinReferencia) {
            throw new ReglaDominioException(
                    "Solo una reserva EXTERNO lleva canal e identificador externo, y los lleva siempre");
        }
    }

    public ReservaId codigo() {
        return codigo;
    }

    public ApartamentoId apartamentoId() {
        return apartamentoId;
    }

    public TitularId titularId() {
        return titularId;
    }

    public Estancia estancia() {
        return estancia;
    }

    public EstadoReserva estado() {
        return estado;
    }

    public CanalOrigen canalOrigen() {
        return canalOrigen;
    }

    public CanalId canalId() {
        return canalId;
    }

    public String idExterno() {
        return idExterno;
    }

    public List<Ocupante> ocupantes() {
        return ocupantes;
    }

    /**
     * El registro de llegada vigente (el único no anulado, REG-06); {@code null} si la estancia no ha iniciado.
     */
    public Registro registro() {
        return registros.stream().filter(r -> !r.anulado()).findFirst().orElse(null);
    }

    public List<Registro> registros() {
        return List.copyOf(registros);
    }

    public Salida salida() {
        return salida;
    }

    public LocalTime horaEstimadaLlegada() {
        return horaEstimadaLlegada;
    }

    public Dinero valorTotal() {
        return valorTotal;
    }

    public List<LineaCotizacion> desglose() {
        return desglose;
    }

    public PoliticaId politicaVersionId() {
        return politicaVersionId;
    }

    public LocalDateTime creadaEn() {
        return creadaEn;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Reserva otra && codigo.equals(otra.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }
}
