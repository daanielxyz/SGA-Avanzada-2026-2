package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

/**
 * VO: documento de identidad (tipo y número no vacío).
 */
public record Documento(TipoDocumento tipo, String numero) {

    public Documento {
        if (tipo == null) {
            throw new ReglaDominioException("El tipo de documento es obligatorio");
        }
        if (numero == null || numero.isBlank()) {
            throw new ReglaDominioException("El número de documento es obligatorio");
        }
        numero = numero.trim();
    }
}
