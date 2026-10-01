package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.CargoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TipoCargo;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad (Hija de Folio): Representa un cargo o débito en la cuenta del huésped.
 */
public class Cargo {

    private final CargoId id;
    private final TipoCargo tipo;
    private final String concepto;
    private final Dinero valor;
    private final LocalDate fecha;

    private Cargo(CargoId id, TipoCargo tipo, String concepto, Dinero valor, LocalDate fecha) {
        this.id = id;
        this.tipo = tipo;
        this.concepto = concepto;
        this.valor = valor;
        this.fecha = fecha;
    }

    public static Cargo crear(CargoId id, TipoCargo tipo, String concepto, Dinero valor, LocalDate fecha) {
        Objects.requireNonNull(id, "El id del cargo no puede ser nulo");
        Objects.requireNonNull(tipo, "El tipo de cargo no puede ser nulo");
        if (concepto == null || concepto.isBlank()) {
            throw new ReglaDominioException("El concepto del cargo no puede estar vacío");
        }
        Objects.requireNonNull(valor, "El valor del cargo no puede ser nulo");
        Objects.requireNonNull(fecha, "La fecha del cargo no puede ser nula");

        return new Cargo(id, tipo, concepto.trim(), valor, fecha);
    }

    public boolean esAjuste() {
        return this.tipo == TipoCargo.AJUSTE;
    }

    public CargoId getId() {
        return id;
    }

    public TipoCargo getTipo() {
        return tipo;
    }

    public String getConcepto() {
        return concepto;
    }

    public Dinero getValor() {
        return valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cargo cargo = (Cargo) o;
        return Objects.equals(id, cargo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
