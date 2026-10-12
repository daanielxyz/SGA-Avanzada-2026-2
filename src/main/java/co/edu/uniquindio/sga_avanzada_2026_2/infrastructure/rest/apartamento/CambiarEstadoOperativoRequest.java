package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CambiarEstadoOperativoCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de {@code PUT /api/apartamentos/{codigo}/estado-operativo}. Un estado que no existe es un error de formato
 * (400); una transición no permitida la rechaza el dominio (409).
 */
public record CambiarEstadoOperativoRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "PREPARADO|OCUPADO|PENDIENTE_PREPARACION|EN_PREPARACION|FUERA_DE_SERVICIO",
                message = "debe ser PREPARADO, OCUPADO, PENDIENTE_PREPARACION, EN_PREPARACION o FUERA_DE_SERVICIO")
        String estado) {

    CambiarEstadoOperativoCommand aCommand(String codigo) {
        return new CambiarEstadoOperativoCommand(codigo, estado);
    }
}
