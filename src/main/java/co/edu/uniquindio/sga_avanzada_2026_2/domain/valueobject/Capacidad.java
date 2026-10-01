package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

/**
 * Objeto de valor: Capacidad máxima de ocupantes de un apartamento.
 */
public record Capacidad(int valor) {

    public Capacidad {
        if (valor < 1) {
            throw new IllegalArgumentException("La capacidad debe ser de al menos 1 ocupante");
        }
    }

    public boolean admite(int ocupantes) {
        return ocupantes > 0 && ocupantes <= valor;
    }
}
