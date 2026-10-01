package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TarifaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TemporadaId;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad Raíz: Agregado Tarifa.
 */
public class Tarifa {

    private final TarifaId id;
    private final ApartamentoId apartamentoId;
    private final TemporadaId temporadaId;
    private final Dinero valorPorOcupante;
    private final int version;
    private final LocalDate vigenteDesde;

    private Tarifa(TarifaId id, ApartamentoId apartamentoId, TemporadaId temporadaId,
                   Dinero valorPorOcupante, int version, LocalDate vigenteDesde) {
        this.id = id;
        this.apartamentoId = apartamentoId;
        this.temporadaId = temporadaId;
        this.valorPorOcupante = valorPorOcupante;
        this.version = version;
        this.vigenteDesde = vigenteDesde;
    }

    public static Tarifa crear(TarifaId id, ApartamentoId apartamentoId, TemporadaId temporadaId,
                               Dinero valorPorOcupante, int version, LocalDate vigenteDesde) {
        Objects.requireNonNull(id, "El id de la tarifa no puede ser nulo");
        Objects.requireNonNull(apartamentoId, "El apartamentoId no puede ser nulo");
        Objects.requireNonNull(temporadaId, "El temporadaId no puede ser nulo");
        Objects.requireNonNull(valorPorOcupante, "El valor por ocupante no puede ser nulo");
        if (version < 1) {
            throw new ReglaDominioException("La versión de la tarifa debe ser mayor o igual a 1");
        }
        Objects.requireNonNull(vigenteDesde, "La fecha de vigencia no puede ser nula");

        return new Tarifa(id, apartamentoId, temporadaId, valorPorOcupante, version, vigenteDesde);
    }

    public Tarifa nuevaVersion(Dinero nuevoValor) {
        Objects.requireNonNull(nuevoValor, "El nuevo valor de tarifa no puede ser nulo");
        return new Tarifa(TarifaId.nuevo(), this.apartamentoId, this.temporadaId,
                nuevoValor, this.version + 1, LocalDate.now());
    }

    public TarifaId getId() {
        return id;
    }

    public ApartamentoId getApartamentoId() {
        return apartamentoId;
    }

    public TemporadaId getTemporadaId() {
        return temporadaId;
    }

    public Dinero getValorPorOcupante() {
        return valorPorOcupante;
    }

    public int getVersion() {
        return version;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tarifa tarifa = (Tarifa) o;
        return Objects.equals(id, tarifa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
