package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.EstadoNovedad;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fila de {@code novedad_cambio}: un paso del ciclo con su autor y fecha (NOV-04).
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CambioNovedadJpa {

    @Enumerated(EnumType.STRING)
    private EstadoNovedad estado;

    private String autor;
    private LocalDateTime fechaHora;
}
