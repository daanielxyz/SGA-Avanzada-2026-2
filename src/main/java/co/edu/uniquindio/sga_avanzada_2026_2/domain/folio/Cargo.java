package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;

import java.time.LocalDate;

/**
 * Entidad interna de Folio. Inmutable: se corrige con un AJUSTE (único tipo que admite valor negativo).
 */
public class Cargo {

    private final CargoId id;
    private final TipoCargo tipo;
    private final String concepto;
    private final Dinero valor;
    private final LocalDate fecha;

    public Cargo(CargoId id, TipoCargo tipo, String concepto, Dinero valor, LocalDate fecha) {
        this.id = id;
        this.tipo = tipo;
        this.concepto = concepto;
        this.valor = valor;
        this.fecha = fecha;
    }
}
