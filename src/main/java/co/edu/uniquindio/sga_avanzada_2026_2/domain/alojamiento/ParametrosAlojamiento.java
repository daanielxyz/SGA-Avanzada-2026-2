package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;

import java.time.Duration;
import java.time.LocalTime;

/**
 * VO: valores configurables por alojamiento. Los valores reales viven en application.properties (sga.*).
 * anticipo = 0 significa que no se exige.
 */
public record ParametrosAlojamiento(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                                    Duration tiempoPreparacion, Duration plazoConfirmacion,
                                    LocalTime horaLimiteNoShow, Porcentaje anticipo) {
}
