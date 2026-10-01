package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

/**
 * Objeto de valor: Porcentaje entre 0 y 100.
 */
public record Porcentaje(int valor) {

    public Porcentaje {
        if (valor < 0 || valor > 100) {
            throw new IllegalArgumentException("El porcentaje debe encontrarse en el rango de 0 a 100, valor recibido: " + valor);
        }
    }

    public static Porcentaje de(int valor) {
        return new Porcentaje(valor);
    }
}
