package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Entidad Raíz: Agregado Folio (cuenta corriente de la reserva).
 */
public class Folio {

    private final FolioId id;
    private final ReservaId reservaId;
    private final List<Cargo> cargos;
    private final List<Pago> pagos;
    private boolean cerrado;
    private AutorizacionCierre autorizacion;

    private Folio(FolioId id, ReservaId reservaId, List<Cargo> cargos, List<Pago> pagos,
                  boolean cerrado, AutorizacionCierre autorizacion) {
        this.id = id;
        this.reservaId = reservaId;
        this.cargos = new ArrayList<>(cargos);
        this.pagos = new ArrayList<>(pagos);
        this.cerrado = cerrado;
        this.autorizacion = autorizacion;
    }

    public static Folio crear(FolioId id, ReservaId reservaId) {
        Objects.requireNonNull(id, "El id del folio no puede ser nulo");
        Objects.requireNonNull(reservaId, "El id de la reserva no puede ser nulo");
        return new Folio(id, reservaId, new ArrayList<>(), new ArrayList<>(), false, null);
    }

    private void validarNoCerrado() {
        if (this.cerrado) {
            throw new ReglaDominioException("El folio se encuentra cerrado y no admite nuevos movimientos");
        }
    }

    public void agregarCargo(Cargo c) {
        validarNoCerrado();
        Objects.requireNonNull(c, "El cargo no puede ser nulo");
        this.cargos.add(c);
    }

    public void registrarPago(Pago p) {
        validarNoCerrado();
        Objects.requireNonNull(p, "El pago no puede ser nulo");
        this.pagos.add(p);
    }

    public Cargo ajustarMovimiento(String motivo, Dinero valorAjuste, LocalDate fecha) {
        validarNoCerrado();
        Objects.requireNonNull(motivo, "El motivo del ajuste no puede ser nulo");
        Objects.requireNonNull(valorAjuste, "El valor de ajuste no puede ser nulo");
        Objects.requireNonNull(fecha, "La fecha del ajuste no puede ser nula");

        Cargo ajuste = Cargo.crear(CargoId.nuevo(), TipoCargo.AJUSTE, motivo, valorAjuste, fecha);
        this.cargos.add(ajuste);
        return ajuste;
    }

    public Dinero saldo() {
        Dinero totalCargos = Dinero.CERO;
        for (Cargo c : cargos) {
            totalCargos = totalCargos.sumar(c.getValor());
        }

        Dinero totalPagos = Dinero.CERO;
        for (Pago p : pagos) {
            if (p.esReverso()) {
                totalPagos = totalPagos.restar(p.getMonto());
            } else {
                totalPagos = totalPagos.sumar(p.getMonto());
            }
        }

        return totalCargos.restar(totalPagos);
    }

    public void cerrar(AutorizacionCierre aut) {
        validarNoCerrado();
        Objects.requireNonNull(aut, "La autorización de cierre es obligatoria");
        this.cerrado = true;
        this.autorizacion = aut;
    }

    public FolioId getId() {
        return id;
    }

    public ReservaId getReservaId() {
        return reservaId;
    }

    public List<Cargo> getCargos() {
        return Collections.unmodifiableList(cargos);
    }

    public List<Pago> getPagos() {
        return Collections.unmodifiableList(pagos);
    }

    public boolean isCerrado() {
        return cerrado;
    }

    public Optional<AutorizacionCierre> getAutorizacion() {
        return Optional.ofNullable(autorizacion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Folio folio = (Folio) o;
        return Objects.equals(id, folio.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
