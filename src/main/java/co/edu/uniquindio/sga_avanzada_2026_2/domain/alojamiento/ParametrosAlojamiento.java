package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.Duration;
import java.time.LocalTime;

/**
 * VO: configuración de negocio editable del alojamiento, nunca constantes del código (ALO-03). Se guarda con el
 * Alojamiento; {@code sga.*} solo aporta los valores iniciales (DEC-25). Cambiarla no altera reservas ya creadas
 * (ALO-04). Horas del día en zona horaria de Colombia (ALO-02).
 *
 * @param anticipo                   0 significa que no se exige anticipo
 * @param minimoMediosPago           mínimo de la Ficha del curso, configurable (ALO-06 · MPAG-01)
 * @param minimoServiciosAdicionales mínimo de la Ficha del curso, configurable (ALO-06 · SERV-05)
 */
public record ParametrosAlojamiento(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                                    Duration tiempoPreparacion, Duration plazoConfirmacion,
                                    LocalTime horaLimiteNoShow, Porcentaje anticipo, int minimoMediosPago,
                                    int minimoServiciosAdicionales) {

    // ALO-02 · ALO-03 · ALO-06 · TPRE-01
    public ParametrosAlojamiento {
        if (horaEntrada == null || horaSalida == null || horaLimiteNoShow == null) {
            throw new ReglaDominioException("Las horas de entrada, salida y límite de no-show son obligatorias");
        }
        if (umbralEdadFacturable < 0) {
            throw new ReglaDominioException("El umbral de edad facturable no puede ser negativo");
        }
        if (tiempoPreparacion == null || tiempoPreparacion.isNegative() || tiempoPreparacion.toSeconds() % 3600 != 0) {
            throw new ReglaDominioException("El tiempo de preparación debe ser un número entero de horas ≥ 0");
        }
        if (plazoConfirmacion == null || plazoConfirmacion.isNegative() || plazoConfirmacion.isZero()) {
            throw new ReglaDominioException("El plazo de confirmación debe ser mayor que cero");
        }
        if (anticipo == null) {
            throw new ReglaDominioException("El anticipo es obligatorio (0 si no se exige)");
        }
        if (minimoMediosPago < 0 || minimoServiciosAdicionales < 0) {
            throw new ReglaDominioException("Los mínimos de medios de pago y servicios no pueden ser negativos");
        }
    }
}
