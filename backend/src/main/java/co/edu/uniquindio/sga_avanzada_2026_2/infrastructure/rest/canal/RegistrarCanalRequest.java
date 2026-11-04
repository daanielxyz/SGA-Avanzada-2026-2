package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarCanalCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/canales} (CU-54). Que solo un canal EXTERNO traiga la referencia de su credencial lo
 * valida el dominio (409).
 *
 * @param referenciaCredencial nombre del secreto en la configuración externa, nunca el secreto (DEC-47)
 */
public record RegistrarCanalRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 80, message = "admite máximo 80 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "PORTAL|DIRECTO|EXTERNO", message = "debe ser PORTAL, DIRECTO o EXTERNO")
        String tipo,

        @Size(max = 120, message = "admite máximo 120 caracteres")
        String referenciaCredencial) {

    RegistrarCanalCommand aCommand(String alojamientoId) {
        return new RegistrarCanalCommand(alojamientoId, nombre, tipo, referenciaCredencial);
    }
}
