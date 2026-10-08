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
 * @param minimoTemporadas           temporadas específicas exigidas, además de la base, para activar
 *                                   apartamentos (TEM-04 · DEC-28)
 * @param minimoTramosCancelacion    tramos que debe tener cada versión de la política de cancelación; mínimo de la
 *                                   Ficha del curso, configurable (POL-01 · DEC-41)
 * @param minimoCapacidadesDistintas capacidades distintas que deben tener los apartamentos activos; mínimo de la
 *                                   Ficha del curso, configurable (CAP-05 · DEC-51)
 */
public record ParametrosAlojamiento(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                                    Duration tiempoPreparacion, Duration plazoConfirmacion,
                                    LocalTime horaLimiteNoShow, Porcentaje anticipo, int minimoMediosPago,
                                    int minimoServiciosAdicionales, int minimoTemporadas,
                                    int minimoTramosCancelacion, int minimoCapacidadesDistintas) {

    // ALO-02 · ALO-03 · ALO-06 · TPRE-01 · TEM-04 · POL-01 · CAP-05
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
        if (minimoMediosPago < 0 || minimoServiciosAdicionales < 0 || minimoTemporadas < 0
                || minimoCapacidadesDistintas < 0) {
            throw new ReglaDominioException(
                    "Los mínimos de medios de pago, servicios, temporadas y capacidades no pueden ser negativos");
        }
        if (minimoTramosCancelacion < 1) {
            throw new ReglaDominioException("La política de cancelación necesita al menos un tramo");
        }
    }
}
