package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado Reserva. Nace PENDIENTE; valor, desglose y política se congelan al crear y solo una modificación
 * explícita los recalcula (RN-22). Disponibilidad, tarifas y estancia mínima llegan resueltas por parámetro desde los
 * servicios de dominio.
 */
public class Reserva {

    private final ReservaId codigo;
    private ApartamentoId apartamentoId;
    private final TitularId titularId;
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

    // OCU-02 · OCU-05 · CORI-03 · RN-22 · REG-01 · REG-06 · SAL-01
    public Reserva(ReservaId codigo, ApartamentoId apartamentoId, TitularId titularId, Estancia estancia,
                   EstadoReserva estado, CanalOrigen canalOrigen, CanalId canalId, String idExterno,
                   List<Ocupante> ocupantes, List<Registro> registros, Salida salida,
                   LocalTime horaEstimadaLlegada, Dinero valorTotal, List<LineaCotizacion> desglose,
                   PoliticaId politicaVersionId, LocalDateTime creadaEn) {
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
    }

    /**
     * Crea una reserva PENDIENTE y congela su valor, desglose y versión de política (RN-22 · RES-22). Que la
     * estancia sea válida (RN-03) lo garantiza {@link Estancia}; que el apartamento esté activo, sin solapes, sin
     * bloqueos y con tiempo de preparación (RN-01 · RN-07 · RN-20) lo verifica antes
     * {@code DisponibilidadDomainService}.
     *
     * @param cotizacion           de {@code TarificacionDomainService} para esta estancia y estos ocupantes
     * @param capacidad            capacidad actual del apartamento (RN-02)
     * @param estanciaMinimaNoches de {@code CalendarioTemporadas.estanciaMinimaPara} (RP-01)
     * @param ahora                fecha y hora actuales en Colombia, inyectadas (EST-06)
     * @throws ReglaDominioException si la entrada es anterior a hoy (RN-04), el grupo excede la capacidad (RN-02),
     *                               no hay ocupantes (OCU-02), hay uno repetido (OCU-05) o nacido en el futuro
     *                               (OCU-12), no se cumple la estancia mínima (RP-01), el canal externo no trae su
     *                               referencia (CORI-03) o la cotización no corresponde a la estancia
     */
    public static Reserva crear(ReservaId codigo, ApartamentoId apartamentoId, TitularId titularId,
                                Estancia estancia, CanalOrigen canalOrigen, CanalId canalId, String idExterno,
                                List<Ocupante> ocupantes, LocalTime horaEstimadaLlegada, Cotizacion cotizacion,
                                PoliticaId politicaVersionId, Capacidad capacidad, int estanciaMinimaNoches,
                                LocalDateTime ahora) {
        Objects.requireNonNull(ahora, "ahora");
        validarCondiciones(estancia, ocupantes, cotizacion, capacidad, estanciaMinimaNoches, ahora.toLocalDate());
        return new Reserva(codigo, apartamentoId, titularId, estancia, EstadoReserva.PENDIENTE, canalOrigen,
                canalId, idExterno, ocupantes, List.of(), null, horaEstimadaLlegada, cotizacion.total(),
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
     * @param hoy                  fecha actual en Colombia, inyectada
     * @throws ReglaDominioException si la reserva no está PENDIENTE ni CONFIRMADA, o si los nuevos datos violan
     *                               alguna condición de creación (RN-04 · RN-02 · OCU-02 · OCU-05 · OCU-12 · RP-01)
     */
    public void modificar(Estancia nuevaEstancia, List<Ocupante> nuevosOcupantes, ApartamentoId nuevoApartamentoId,
                          Cotizacion cotizacion, Capacidad capacidad, int estanciaMinimaNoches, LocalDate hoy) {
        if (estado != EstadoReserva.PENDIENTE && estado != EstadoReserva.CONFIRMADA) {
            throw new ReglaDominioException("Solo se modifica una reserva PENDIENTE o CONFIRMADA; está " + estado);
        }
        Objects.requireNonNull(nuevoApartamentoId, "nuevoApartamentoId");
        Objects.requireNonNull(hoy, "hoy");
        validarCondiciones(nuevaEstancia, nuevosOcupantes, cotizacion, capacidad, estanciaMinimaNoches, hoy);
        estancia = nuevaEstancia;
        ocupantes = List.copyOf(nuevosOcupantes);
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
     * anticipo, que los pagos cubran ese porcentaje del valor congelado (RES-15).
     *
     * @param anticipo {@code ParametrosAlojamiento.anticipo}; 0 = no se exige
     * @param pagado   total pagado en el folio de la reserva
     * @throws ReglaDominioException si no está PENDIENTE (RN-08), falta la hora estimada (RN-09) o el pago no cubre
     *                               el anticipo (RES-15)
     */
    public void confirmar(Porcentaje anticipo, Dinero pagado) {
        Objects.requireNonNull(anticipo, "anticipo");
        Objects.requireNonNull(pagado, "pagado");
        validarTransicion(EstadoReserva.CONFIRMADA);
        if (horaEstimadaLlegada == null) {
            throw new ReglaDominioException("La reserva necesita hora estimada de llegada para confirmarse");
        }
        Dinero exigido = valorTotal.porcentaje(anticipo);
        if (pagado.esMenorQue(exigido)) {
            throw new ReglaDominioException("El pago (" + pagado.monto() + ") no cubre el anticipo exigido ("
                    + exigido.monto() + ")");
        }
        estado = EstadoReserva.CONFIRMADA;
    }

    /**
     * Cancela una reserva que no ha iniciado y libera sus noches de inmediato (RN-08 · RN-12). La retención según la
     * política congelada la calcula {@code CancelacionDomainService} (RN-13, A5).
     *
     * @throws ReglaDominioException si no está PENDIENTE ni CONFIRMADA (RN-08)
     */
    public void cancelar() {
        validarTransicion(EstadoReserva.CANCELADA);
        estado = EstadoReserva.CANCELADA;
    }

    /**
     * Declara que el titular no se presentó y libera las noches (RN-08 · RN-12). Solo a partir de la hora límite
     * del día de entrada (RES-16).
     *
     * @param ahora      fecha y hora actuales en Colombia, inyectadas
     * @param horaLimite {@code ParametrosAlojamiento.horaLimiteNoShow}
     * @throws ReglaDominioException si no está CONFIRMADA (RN-08) o aún no llega la hora límite (RES-16)
     */
    public void declararNoShow(LocalDateTime ahora, LocalTime horaLimite) {
        Objects.requireNonNull(ahora, "ahora");
        Objects.requireNonNull(horaLimite, "horaLimite");
        validarTransicion(EstadoReserva.NO_SHOW);
        LocalDateTime limite = estancia.entrada().atTime(horaLimite);
        if (ahora.isBefore(limite)) {
            throw new ReglaDominioException("El no-show solo puede declararse desde " + limite);
        }
        estado = EstadoReserva.NO_SHOW;
    }

    /**
     * Cancela automáticamente una reserva PENDIENTE que superó el plazo de confirmación contado desde su creación
     * (RN-21 · RES-17 · RES-21). Lo invoca el caso de uso {@code VencerReservasPendientes}.
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @param plazo {@code ParametrosAlojamiento.plazoConfirmacion}
     * @throws ReglaDominioException si no está PENDIENTE o todavía no supera el plazo
     */
    public void expirar(LocalDateTime ahora, Duration plazo) {
        Objects.requireNonNull(ahora, "ahora");
        Objects.requireNonNull(plazo, "plazo");
        if (estado != EstadoReserva.PENDIENTE) {
            throw new ReglaDominioException("Solo vence una reserva PENDIENTE; está " + estado);
        }
        if (!ahora.isAfter(creadaEn.plus(plazo))) {
            throw new ReglaDominioException("La reserva " + codigo.valor() + " no ha superado el plazo de confirmación");
        }
        estado = EstadoReserva.CANCELADA;
    }

    /**
     * Registra la llegada (check-in) con la fecha, hora y autor reales (REG-05) y pasa a EN_CURSO (RN-08). Que el
     * apartamento esté PREPARADO y pase a OCUPADO en la misma transacción lo coordina
     * {@code RegistroLlegadaDomainService} (RN-11 · REG-04 · D-02).
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si no está CONFIRMADA (RN-10 · REG-01) o aún no es la fecha de entrada
     *                               (RN-10 · REG-02)
     */
    public void registrarLlegada(RegistroId id, LocalDateTime ahora, UsuarioId autor) {
        Objects.requireNonNull(ahora, "ahora");
        if (estado != EstadoReserva.CONFIRMADA) {
            throw new ReglaDominioException("Solo se registra la llegada de una reserva CONFIRMADA; está " + estado);
        }
        if (ahora.toLocalDate().isBefore(estancia.entrada())) {
            throw new ReglaDominioException("No se puede registrar la llegada antes de la fecha de entrada "
                    + estancia.entrada());
        }
        registros.add(new Registro(id, ahora, autor, false, null));
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
            throw new ReglaDominioException("Solo se corrige el registro de una reserva EN_CURSO; está " + estado);
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
        Registro corregido = new Registro(nuevoId, fechaHoraCorrecta, autor, false, motivo);
        registro().anular();
        registros.add(corregido);
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
     * @throws ReglaDominioException si no está EN_CURSO (SAL-01) o la hora es anterior a la del registro (SAL-05)
     */
    public void validarSalida(LocalDateTime ahora) {
        Objects.requireNonNull(ahora, "ahora");
        if (estado != EstadoReserva.EN_CURSO) {
            throw new ReglaDominioException("Solo se registra la salida de una reserva EN_CURSO; está " + estado);
        }
        if (ahora.isBefore(registro().fechaHora())) {
            throw new ReglaDominioException("La salida no puede ser anterior al registro de llegada");
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
            throw new ReglaDominioException("Transición de reserva no permitida: " + estado + " → " + destino);
        }
    }

    // Condiciones de crear y de modificar (RN-14): RN-04 · RN-02 · OCU-02 · OCU-05 · OCU-12 · RP-01
    private static void validarCondiciones(Estancia estancia, List<Ocupante> ocupantes, Cotizacion cotizacion,
                                           Capacidad capacidad, int estanciaMinimaNoches, LocalDate hoy) {
        Objects.requireNonNull(estancia, "estancia");
        Objects.requireNonNull(cotizacion, "cotizacion");
        Objects.requireNonNull(capacidad, "capacidad");
        if (estancia.entrada().isBefore(hoy)) {
            throw new ReglaDominioException("La fecha de entrada no puede ser anterior a hoy (" + hoy + ")");
        }
        validarGrupo(ocupantes);
        if (!capacidad.admite(ocupantes.size())) {
            throw new ReglaDominioException("El grupo de " + ocupantes.size()
                    + " ocupantes excede la capacidad del apartamento (" + capacidad.valor() + ")");
        }
        ocupantes.stream()
                .filter(o -> o.fechaNacimiento().isAfter(hoy))
                .findFirst()
                .ifPresent(o -> {
                    throw new ReglaDominioException("La fecha de nacimiento de " + o.nombre() + " es futura");
                });
        if (estancia.cantidadNoches() < estanciaMinimaNoches) {
            throw new ReglaDominioException("La estancia debe ser de al menos " + estanciaMinimaNoches
                    + " noches en esta temporada");
        }
        if (!cotizacion.desglose().stream().map(LineaCotizacion::noche).toList().equals(estancia.noches())) {
            throw new ReglaDominioException("La cotización no corresponde a las noches de la estancia");
        }
    }

    // OCU-02 · OCU-05
    private static void validarGrupo(List<Ocupante> ocupantes) {
        Objects.requireNonNull(ocupantes, "ocupantes");
        if (ocupantes.isEmpty()) {
            // TODO(equipo): que el titular sea uno de los ocupantes (OCU-02 · TIT-01) se verifica con Titular en A7
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
