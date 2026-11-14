package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fila de {@code reserva_correccion_documento}: un documento de un ocupante o del titular corregido (DEC-120).
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CorreccionDocumentoJpa {

    private String ocupante;
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoAnterior;
    private String numeroAnterior;
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoNuevo;
    private String numeroNuevo;
    private String motivo;
    private String autor;
    private LocalDateTime fechaHora;
    private boolean titular;
}
