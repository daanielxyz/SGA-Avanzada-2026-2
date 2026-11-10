package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDate;

/**
 * Entidad interna de Folio, inmutable: su valor queda congelado al registrarse (CAR-05) y se corrige con un AJUSTE
 * en sentido contrario, nunca editándolo (CAR-03 · FOL-04). El valor siempre es positivo; solo el AJUSTE puede
 * disminuir el saldo (CAR-06 · DEC-20).
 */
public class Cargo {

    private final CargoId id;
    private final TipoCargo tipo;
    private final String concepto;
    private final Dinero valor;          // > 0
    private final SentidoAjuste sentido; // solo AJUSTE
    private final LocalDate fecha;
    private final CargoId corrigeA;      // opcional: el cargo que este AJUSTE revierte
    private final UsuarioId autor;       // solo en una corrección hecha por una persona (DEC-100)
    private final int cantidad;          // > 1 solo en un servicio adicional («dos aguas», BL-12); el valor es el total

    public Cargo(CargoId id, TipoCargo tipo, String concepto, Dinero valor, SentidoAjuste sentido, LocalDate fecha,
                 CargoId corrigeA) {
        this(id, tipo, concepto, valor, sentido, fecha, corrigeA, null);
    }

    public Cargo(CargoId id, TipoCargo tipo, String concepto, Dinero valor, SentidoAjuste sentido, LocalDate fecha,
                 CargoId corrigeA, UsuarioId autor) {
        this(id, tipo, concepto, valor, sentido, fecha, corrigeA, autor, 1);
    }

    // CAR-01 · CAR-06 · TCAR-02 · BL-12
    public Cargo(CargoId id, TipoCargo tipo, String concepto, Dinero valor, SentidoAjuste sentido, LocalDate fecha,
                 CargoId corrigeA, UsuarioId autor, int cantidad) {
        if (id == null || tipo == null || valor == null || fecha == null) {
            throw new ReglaDominioException("El cargo requiere id, tipo, valor y fecha");
        }
        if (concepto == null || concepto.isBlank()) {
            throw new ReglaDominioException("El concepto del cargo es obligatorio");
        }
        if (!valor.esPositivo()) {
            throw new ReglaDominioException("El valor del cargo debe ser mayor que cero");
        }
        boolean esAjuste = tipo == TipoCargo.AJUSTE;
        if (esAjuste != (sentido != null)) {
            throw new ReglaDominioException("Solo un AJUSTE lleva sentido, y siempre lo lleva");
        }
        if (!esAjuste && corrigeA != null) {
            throw new ReglaDominioException("Solo un AJUSTE puede corregir otro cargo");
        }
        if (cantidad < 1 || (cantidad > 1 && tipo != TipoCargo.SERVICIO_ADICIONAL)) {
            throw new ReglaDominioException("La cantidad es al menos 1, y solo un servicio adicional lleva más de una");
        }
        this.id = id;
        this.tipo = tipo;
        this.concepto = concepto.trim();
        this.valor = valor;
        this.sentido = sentido;
        this.fecha = fecha;
        this.corrigeA = corrigeA;
        this.autor = autor;
        this.cantidad = cantidad;
    }

    /** Cuántas unidades cobra; el valor ya es el total (BL-12). */
    public int cantidad() {
        return cantidad;
    }

    /**
     * Indica si el cargo sube el saldo: todos los tipos lo suben salvo un AJUSTE que disminuye (TCAR-03 · CAR-06).
     */
    public boolean aumentaSaldo() {
        return sentido != SentidoAjuste.DISMINUYE;
    }

    public boolean esAjuste() {
        return tipo == TipoCargo.AJUSTE;
    }

    public CargoId id() {
        return id;
    }

    public TipoCargo tipo() {
        return tipo;
    }

    public String concepto() {
        return concepto;
    }

    public Dinero valor() {
        return valor;
    }

    public SentidoAjuste sentido() {
        return sentido;
    }

    public LocalDate fecha() {
        return fecha;
    }

    public CargoId corrigeA() {
        return corrigeA;
    }

    public UsuarioId autor() {
        return autor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Cargo otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
