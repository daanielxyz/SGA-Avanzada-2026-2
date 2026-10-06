package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado Folio, uno por reserva (FOL-02). Cargos y pagos solo se agregan; se corrigen con un movimiento
 * inverso (RN-16 · FOL-04). El saldo se calcula, nunca se guarda (RN-15 · FOL-03). Los ids de sus movimientos los
 * numera el propio folio (DEC-43).
 */
public class Folio {

    private final FolioId id;
    private final ReservaId reservaId;
    private final List<Cargo> cargos;
    private final List<Pago> pagos;
    private boolean cerrado;
    private AutorizacionCierre autorizacion; // solo si cerró con saldo distinto de cero

    // FOL-02 · FOL-05
    public Folio(FolioId id, ReservaId reservaId, List<Cargo> cargos, List<Pago> pagos, boolean cerrado,
                 AutorizacionCierre autorizacion) {
        if (id == null || reservaId == null || cargos == null || pagos == null) {
            throw new ReglaDominioException("Faltan datos obligatorios del folio");
        }
        if (autorizacion != null && !cerrado) {
            throw new ReglaDominioException("Solo un folio cerrado tiene autorización de cierre");
        }
        this.id = id;
        this.reservaId = reservaId;
        this.cargos = new ArrayList<>(cargos);
        this.pagos = new ArrayList<>(pagos);
        this.cerrado = cerrado;
        this.autorizacion = autorizacion;
    }

    /**
     * Abre el folio al crear la reserva con el cargo de alojamiento ya calculado, para poder registrar el anticipo
     * antes de la llegada (FOL-01 · CAR-04).
     *
     * @param valorAlojamiento valor congelado de la reserva (RN-22)
     * @throws ReglaDominioException si el valor no es positivo (CAR-06)
     */
    public static Folio abrir(FolioId id, ReservaId reservaId, Dinero valorAlojamiento, LocalDate fecha) {
        Folio folio = new Folio(id, reservaId, List.of(), List.of(), false, null);
        folio.cargos.add(new Cargo(folio.siguienteCargo(), TipoCargo.HOSPEDAJE, "Alojamiento", valorAlojamiento,
                null, fecha, null));
        return folio;
    }

    /**
     * Agrega el cargo de un servicio adicional con su valor del momento, que queda congelado (CAR-05).
     *
     * @throws ReglaDominioException si el folio está cerrado (CAR-07) o el valor no es positivo (CAR-06)
     */
    public CargoId agregarServicioAdicional(String concepto, Dinero valor, LocalDate fecha) {
        return agregar(TipoCargo.SERVICIO_ADICIONAL, concepto, valor, null, fecha, null);
    }

    /**
     * Registra un abono (RN-15 · PAG-01). Admite varios pagos parciales con medios distintos (PAG-05).
     *
     * @param hoy                fecha actual en Colombia, inyectada
     * @param mediosHabilitados  catálogo vigente del alojamiento ({@code Alojamiento.mediosPago})
     * @throws ReglaDominioException si el folio está cerrado (FOL-06), falta medio o fecha (PAG-01), el monto no es
     *                               positivo (PAG-02), la fecha es futura (PAG-07) o el medio no está habilitado
     *                               (MPAG-02)
     */
    public PagoId registrarPago(MedioPago medio, Dinero monto, LocalDate fecha, LocalDate hoy,
                                Collection<MedioPago> mediosHabilitados) {
        validarMovimientoDePago(medio, fecha, hoy, mediosHabilitados);
        return agregarPago(medio, monto, TipoPago.ABONO, fecha, null);
    }

    /**
     * Registra una devolución como REVERSO contra el folio; el desembolso ocurre fuera del sistema (PAG-06 ·
     * TPAG-02). La registra recepción indicando el medio por el que devuelve (DEC-43).
     *
     * @throws ReglaDominioException si el folio está cerrado (FOL-06), el medio no está habilitado (MPAG-02), la
     *                               fecha es futura (PAG-07) o el monto supera lo abonado neto (TPAG-02)
     */
    public PagoId registrarDevolucion(MedioPago medio, Dinero monto, LocalDate fecha, LocalDate hoy,
                                      Collection<MedioPago> mediosHabilitados) {
        validarMovimientoDePago(medio, fecha, hoy, mediosHabilitados);
        validarNoSuperaLoAbonado(monto);
        return agregarPago(medio, monto, TipoPago.REVERSO, fecha, null);
    }

    /**
     * Corrige un abono mal registrado con un REVERSO del mismo medio y monto que lo referencia (RN-16 · PAG-03 ·
     * TPAG-02). El abono original no se edita ni se borra.
     *
     * @throws ReglaDominioException si el folio está cerrado (FOL-06), el pago no existe, no es un abono, ya fue
     *                               revertido, la fecha es futura (PAG-07) o el monto supera lo abonado neto
     */
    public PagoId revertirPago(PagoId pagoId, LocalDate fecha, LocalDate hoy) {
        validarAbierto();
        validarFechaNoFutura(fecha, hoy);
        Pago original = pagos.stream()
                .filter(p -> p.id().equals(pagoId))
                .findFirst()
                .orElseThrow(() -> new ReglaDominioException("El folio no tiene el pago " + pagoId.valor()));
        if (original.esReverso()) {
            throw new ReglaDominioException("Un reverso no se revierte; registre un nuevo abono");
        }
        if (pagos.stream().anyMatch(p -> pagoId.equals(p.reversaDe()))) {
            throw new ReglaDominioException("El pago " + pagoId.valor() + " ya fue revertido");
        }
        validarNoSuperaLoAbonado(original.monto());
        return agregarPago(original.medio(), original.monto(), TipoPago.REVERSO, fecha, pagoId);
    }

    /**
     * Corrige un cargo mal registrado con un AJUSTE del mismo valor en sentido contrario que lo referencia (RN-16 ·
     * CAR-03 · FOL-04). El cargo original no se edita ni se borra.
     *
     * @throws ReglaDominioException si el folio está cerrado (CAR-07), el cargo no existe, ya fue corregido o es él
     *                               mismo una corrección
     */
    public CargoId revertirCargo(CargoId cargoId, String motivo, LocalDate fecha) {
        validarAbierto();
        Cargo original = cargos.stream()
                .filter(c -> c.id().equals(cargoId))
                .findFirst()
                .orElseThrow(() -> new ReglaDominioException("El folio no tiene el cargo " + cargoId.valor()));
        if (original.corrigeA() != null) {
            throw new ReglaDominioException("Una corrección no se revierte; registre un cargo nuevo");
        }
        if (cargos.stream().anyMatch(c -> cargoId.equals(c.corrigeA()))) {
            throw new ReglaDominioException("El cargo " + cargoId.valor() + " ya fue corregido");
        }
        SentidoAjuste contrario = original.aumentaSaldo() ? SentidoAjuste.DISMINUYE : SentidoAjuste.AUMENTA;
        return agregar(TipoCargo.AJUSTE, "Corrección de " + cargoId.valor() + ": " + motivo, original.valor(),
                contrario, fecha, cargoId);
    }

    /**
     * Registra la diferencia de una modificación de la reserva como AJUSTE: aumenta si el valor nuevo es mayor,
     * disminuye si es menor y no registra nada si es igual (RN-14 · OCU-06).
     *
     * @param valorAnterior valor congelado antes de {@code Reserva.modificar}
     * @param valorNuevo    valor congelado después
     * @throws ReglaDominioException si el folio está cerrado (CAR-07)
     */
    public void ajustarPorModificacion(Dinero valorAnterior, Dinero valorNuevo, LocalDate fecha) {
        validarAbierto();
        if (valorNuevo.esMenorQue(valorAnterior)) {
            agregar(TipoCargo.AJUSTE, "Modificación de la reserva", valorAnterior.restar(valorNuevo),
                    SentidoAjuste.DISMINUYE, fecha, null);
        } else if (valorAnterior.esMenorQue(valorNuevo)) {
            agregar(TipoCargo.AJUSTE, "Modificación de la reserva", valorNuevo.restar(valorAnterior),
                    SentidoAjuste.AUMENTA, fecha, null);
        }
    }

    /**
     * Liquida una cancelación o un no-show: anula el alojamiento con un AJUSTE y registra lo retenido como cargo de
     * penalidad, para que el ingreso por penalidades se vea aparte (RN-13 · CAR-02 · DEC-43). Si lo pagado supera la
     * penalidad, el saldo queda a favor y recepción registra la devolución (PAG-06).
     *
     * @param valorEstancia valor congelado de la reserva, que deja de cobrarse
     * @param penalidad     lo retenido según la política congelada; cero si no hay penalidad
     * @param motivo        «Cancelación» o «No-show»
     * @throws ReglaDominioException si el folio está cerrado (CAR-07)
     */
    public void liquidarPenalidad(Dinero valorEstancia, Dinero penalidad, String motivo, LocalDate fecha) {
        validarAbierto();
        agregar(TipoCargo.AJUSTE, "Anulación del alojamiento por " + motivo, valorEstancia,
                SentidoAjuste.DISMINUYE, fecha, null);
        if (penalidad.esPositivo()) {
            agregar(TipoCargo.PENALIDAD_CANCELACION, "Penalidad por " + motivo, penalidad, null, fecha, null);
        }
    }

    /**
     * Saldo = cargos (los ajustes que disminuyen restan) − pagos netos (abonos − reversos). Se recalcula con cada
     * movimiento y nunca se escribe (RN-15 · FOL-03 · SLD-01 · SLD-02).
     *
     * @return positivo = debe el huésped, a favor = negativo, al día = cero (SLD-03)
     */
    public Saldo saldo() {
        BigDecimal cargosNetos = cargos.stream()
                .map(c -> c.aumentaSaldo() ? c.valor().monto() : c.valor().monto().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Saldo.de(cargosNetos.subtract(totalPagado().monto()));
    }

    /**
     * Pagos netos: abonos − reversos. Nunca es negativo porque un reverso no puede superar lo abonado (TPAG-02).
     * Alimenta el anticipo al confirmar (RES-15) y la base PAGADO de una penalización (DEC-41).
     */
    public Dinero totalPagado() {
        BigDecimal neto = pagos.stream()
                .map(p -> p.esReverso() ? p.monto().monto().negate() : p.monto().monto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Dinero(neto);
    }

    /**
     * Cierra el folio con saldo en cero (RN-17 · FOL-05).
     *
     * @throws ReglaDominioException si ya está cerrado o el saldo no es cero
     */
    public void cerrar() {
        validarAbierto();
        Saldo saldo = saldo();
        if (!saldo.alDia()) {
            throw new ReglaDominioException("El folio tiene saldo " + saldo.situacion() + " de " + saldo.monto().monto()
                    + ": solo se cierra con autorización del administrador");
        }
        cerrado = true;
    }

    /**
     * Cierra el folio con saldo distinto de cero gracias a una autorización explícita registrada con autor y motivo
     * (RN-17 · FOL-05). Que el autor sea administrador lo controla la aplicación (DEC-22).
     *
     * @throws ReglaDominioException si ya está cerrado
     */
    public void cerrar(AutorizacionCierre autorizacionCierre) {
        Objects.requireNonNull(autorizacionCierre, "autorizacionCierre");
        validarAbierto();
        autorizacion = autorizacionCierre;
        cerrado = true;
    }

    private CargoId agregar(TipoCargo tipo, String concepto, Dinero valor, SentidoAjuste sentido, LocalDate fecha,
                            CargoId corrigeA) {
        validarAbierto();
        CargoId nuevo = siguienteCargo();
        cargos.add(new Cargo(nuevo, tipo, concepto, valor, sentido, fecha, corrigeA));
        return nuevo;
    }

    private PagoId agregarPago(MedioPago medio, Dinero monto, TipoPago tipo, LocalDate fecha, PagoId reversaDe) {
        PagoId nuevo = new PagoId("PAG-" + (pagos.size() + 1));
        pagos.add(new Pago(nuevo, medio, monto, tipo, fecha, reversaDe));
        return nuevo;
    }

    // DEC-43: ids correlativos dentro del folio; los movimientos nunca se borran, así que no se repiten
    private CargoId siguienteCargo() {
        return new CargoId("CAR-" + (cargos.size() + 1));
    }

    // FOL-06 · CAR-07
    private void validarAbierto() {
        if (cerrado) {
            throw new ReglaDominioException("El folio " + id.valor() + " está cerrado: no admite movimientos");
        }
    }

    // PAG-01 · PAG-07 · MPAG-02
    private void validarMovimientoDePago(MedioPago medio, LocalDate fecha, LocalDate hoy,
                                         Collection<MedioPago> mediosHabilitados) {
        validarAbierto();
        if (medio == null || fecha == null) {
            throw new ReglaDominioException("Todo pago indica medio de pago y fecha");
        }
        validarFechaNoFutura(fecha, hoy);
        if (!mediosHabilitados.contains(medio)) {
            throw new ReglaDominioException("El medio de pago " + medio.nombre() + " no está habilitado");
        }
    }

    // PAG-07
    private static void validarFechaNoFutura(LocalDate fecha, LocalDate hoy) {
        Objects.requireNonNull(hoy, "hoy");
        if (fecha.isAfter(hoy)) {
            throw new ReglaDominioException("La fecha del pago no puede ser posterior a hoy (" + hoy + ")");
        }
    }

    // TPAG-02
    private void validarNoSuperaLoAbonado(Dinero monto) {
        Objects.requireNonNull(monto, "monto");
        if (totalPagado().esMenorQue(monto)) {
            throw new ReglaDominioException("El reverso (" + monto.monto() + ") supera lo abonado neto ("
                    + totalPagado().monto() + ")");
        }
    }

    public FolioId id() {
        return id;
    }

    public ReservaId reservaId() {
        return reservaId;
    }

    public List<Cargo> cargos() {
        return List.copyOf(cargos);
    }

    public List<Pago> pagos() {
        return List.copyOf(pagos);
    }

    public boolean cerrado() {
        return cerrado;
    }

    public AutorizacionCierre autorizacion() {
        return autorizacion;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Folio otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
