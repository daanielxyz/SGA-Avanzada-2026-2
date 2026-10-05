package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Raíz del agregado Tarifa: valor por ocupante facturable, por noche, de un apartamento en una temporada (TAR-01).
 * Cada instancia es una versión inmutable; cambiar el valor crea otra y conserva el histórico (TAR-05).
 */
// TODO(equipo): valor de la tarifa de Temporada Media por definir (dato inicial, no regla).
public class Tarifa {

    private final TarifaId id;
    private final ApartamentoId apartamentoId;
    private final TemporadaId temporadaId;
    private final Dinero valorPorOcupante;
    private final int version;
    private final LocalDate vigenteDesde;

    // TAR-01 · TAR-02 · TAR-05
    public Tarifa(TarifaId id, ApartamentoId apartamentoId, TemporadaId temporadaId, Dinero valorPorOcupante,
                  int version, LocalDate vigenteDesde) {
        if (id == null || apartamentoId == null || temporadaId == null || valorPorOcupante == null
                || vigenteDesde == null) {
            throw new ReglaDominioException("La tarifa requiere id, apartamento, temporada, valor y vigencia");
        }
        if (!valorPorOcupante.esPositivo()) {
            throw new ReglaDominioException("El valor de la tarifa debe ser mayor que cero");
        }
        if (version < 1) {
            throw new ReglaDominioException("La versión de la tarifa empieza en 1");
        }
        this.id = id;
        this.apartamentoId = apartamentoId;
        this.temporadaId = temporadaId;
        this.valorPorOcupante = valorPorOcupante;
        this.version = version;
        this.vigenteDesde = vigenteDesde;
    }

    /**
     * Crea la siguiente versión con un valor nuevo. Esta versión queda como histórico y las reservas ya creadas
     * conservan su valor congelado (TAR-05 · RN-22).
     *
     * @param desde fecha desde la que rige la nueva versión
     * @return una Tarifa nueva con versión + 1 para el mismo apartamento y temporada
     * @throws ReglaDominioException si {@code desde} es anterior a la vigencia de esta versión o el valor no es
     *                               positivo (TAR-02)
     */
    public Tarifa nuevaVersion(TarifaId nuevoId, Dinero valor, LocalDate desde) {
        Objects.requireNonNull(desde, "desde");
        if (desde.isBefore(vigenteDesde)) {
            throw new ReglaDominioException("La nueva versión no puede regir antes que la actual");
        }
        return new Tarifa(nuevoId, apartamentoId, temporadaId, valor, version + 1, desde);
    }

    /**
     * Indica si esta tarifa es del apartamento y la temporada dados.
     */
    public boolean aplicaA(ApartamentoId apartamento, TemporadaId temporada) {
        return apartamentoId.equals(apartamento) && temporadaId.equals(temporada);
    }

    public TarifaId id() {
        return id;
    }

    public ApartamentoId apartamentoId() {
        return apartamentoId;
    }

    public TemporadaId temporadaId() {
        return temporadaId;
    }

    public Dinero valorPorOcupante() {
        return valorPorOcupante;
    }

    public int version() {
        return version;
    }

    public LocalDate vigenteDesde() {
        return vigenteDesde;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tarifa otra && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
