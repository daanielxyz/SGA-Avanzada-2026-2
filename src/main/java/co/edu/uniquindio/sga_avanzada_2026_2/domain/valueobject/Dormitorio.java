package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

/**
 * Objeto de valor: Cantidad de dormitorios de un apartamento.
 */
public record Dormitorio(int cantidad) {

    public Dormitorio {
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad de dormitorios debe ser al menos 1");
        }
    }
}
