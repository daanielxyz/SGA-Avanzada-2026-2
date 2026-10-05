package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamentoCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/apartamentos}. Valida solo el formato (400); las reglas de negocio las valida el
 * dominio (409) (DEC-31).
 */
public record CrearApartamentoRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "APT-\\d+", message = "debe tener el formato APT-101")
        String codigo,

        @NotBlank(message = "es obligatorio")
        String alojamientoId,

        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @Size(max = 2000, message = "admite máximo 2000 caracteres")
        String descripcion,

        @NotNull(message = "es obligatoria")
        @Min(value = 1, message = "debe ser al menos 1")
        Integer capacidad,

        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        Integer dormitorios,

        @NotNull(message = "es obligatoria")
        @Size(max = 10, message = "admite máximo 10 imágenes")
        List<@Valid @NotNull(message = "no puede ser nula") ImagenRequest> imagenes,

        @NotEmpty(message = "debe tener al menos una característica")
        List<@NotBlank(message = "no puede estar vacía") @Size(max = 100, message = "admite máximo 100 caracteres") String> caracteristicas) {

    public record ImagenRequest(
            @NotBlank(message = "es obligatoria")
            @Pattern(regexp = "https?://\\S+", message = "debe ser una URL http(s)")
            @Size(max = 500, message = "admite máximo 500 caracteres")
            String url,

            boolean principal) {
    }

    CrearApartamentoCommand aCommand() {
        return new CrearApartamentoCommand(codigo, alojamientoId, nombre, descripcion, capacidad, dormitorios,
                imagenes.stream().map(i -> new CrearApartamentoCommand.ImagenCommand(i.url(), i.principal())).toList(),
                caracteristicas);
    }
}
