package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ActualizarApartamentoCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo de {@code PUT /api/apartamentos/{codigo}} (CU-31). La capacidad se cambia aparte (APA-15).
 */
public record ActualizarApartamentoRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        String descripcion,

        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        Integer dormitorios,

        @NotEmpty(message = "debe tener al menos una característica")
        List<@NotBlank(message = "no puede estar vacía") String> caracteristicas) {

    ActualizarApartamentoCommand aCommand(String codigo) {
        return new ActualizarApartamentoCommand(codigo, nombre, descripcion, dormitorios, caracteristicas);
    }
}
