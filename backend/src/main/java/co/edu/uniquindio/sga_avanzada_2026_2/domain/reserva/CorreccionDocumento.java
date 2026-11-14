package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * VO: un documento de un ocupante o del titular corregido porque se escribió mal (#14 · DEC-120). Queda en la
 * reserva, no en el ocupante, porque los ids de los ocupantes pueden cambiar al modificar; por eso guarda el nombre
 * que tenía.
 *
 * @param ocupante nombre de la persona cuando se corrigió
 * @param autor    quien corrigió, de la sesión (DEC-61)
 * @param titular  si fue el documento del titular de la reserva (y, si se aloja, el de su ocupante con él)
 */
public record CorreccionDocumento(String ocupante, Documento anterior, Documento nuevo, String motivo,
                                  UsuarioId autor, LocalDateTime fechaHora, boolean titular) {

    /** La de un ocupante que no es el titular. */
    public CorreccionDocumento(String ocupante, Documento anterior, Documento nuevo, String motivo, UsuarioId autor,
                               LocalDateTime fechaHora) {
        this(ocupante, anterior, nuevo, motivo, autor, fechaHora, false);
    }

    public CorreccionDocumento {
        if (ocupante == null || anterior == null || nuevo == null || autor == null || fechaHora == null) {
            throw new ReglaDominioException("La corrección del documento requiere ocupante, documentos, autor y fecha");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("Escribe por qué se corrige el documento");
        }
        motivo = motivo.trim();
    }
}
