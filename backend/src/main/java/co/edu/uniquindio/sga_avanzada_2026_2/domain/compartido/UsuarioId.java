package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

/**
 * Id tipado: usuario interno (autor de registros, salidas, autorizaciones).
 */
public record UsuarioId(String valor) {

    public UsuarioId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("UsuarioId es obligatorio");
        }
    }
}
