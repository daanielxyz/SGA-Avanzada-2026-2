package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.RegistrarNovedadCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/apartamentos/{codigo}/novedades} (CU-43).
 */
public record RegistrarNovedadRequest(
        @NotBlank(message = "es obligatoria")
        @Size(max = 2000, message = "admite máximo 2000 caracteres")
        String descripcion,

        // TODO(equipo): los valores de gravedad siguen por confirmar (NOV-02 · DEC-09).
        @NotBlank(message = "es obligatoria")
        @Pattern(regexp = "BAJA|MEDIA|ALTA", message = "debe ser BAJA, MEDIA o ALTA")
        String gravedad) {

    RegistrarNovedadCommand aCommand(String apartamento, String autor) {
        return new RegistrarNovedadCommand(apartamento, descripcion, gravedad, autor);
    }
}
