package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.apartamento;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Fila de {@code apartamento_imagen}.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ImagenJpa {

    private String url;
    private boolean principal;
}
