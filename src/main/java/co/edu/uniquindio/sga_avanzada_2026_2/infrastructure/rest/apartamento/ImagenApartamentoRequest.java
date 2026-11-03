package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ImagenApartamentoCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de las operaciones sobre una imagen del apartamento (CU-31): su url la identifica (IMG-04).
 */
public record ImagenApartamentoRequest(@NotBlank(message = "es obligatoria") String url) {

    ImagenApartamentoCommand aCommand(String codigo) {
        return new ImagenApartamentoCommand(codigo, url);
    }
}
