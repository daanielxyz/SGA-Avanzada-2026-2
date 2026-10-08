package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Parámetros de negocio del alojamiento como datos simples (DEC-31); las duraciones van en horas (RES-17 · TPRE-01).
 */
public record ParametrosCommand(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                                int tiempoPreparacionHoras, int plazoConfirmacionHoras, LocalTime horaLimiteNoShow,
                                int anticipoPct, int minimoMediosPago, int minimoServiciosAdicionales,
                                int minimoTemporadas, int minimoTramosCancelacion, int minimoCapacidadesDistintas) {

    ParametrosAlojamiento aDominio() {
        return new ParametrosAlojamiento(umbralEdadFacturable, horaEntrada, horaSalida,
                Duration.ofHours(tiempoPreparacionHoras), Duration.ofHours(plazoConfirmacionHoras), horaLimiteNoShow,
                new Porcentaje(anticipoPct), minimoMediosPago, minimoServiciosAdicionales, minimoTemporadas,
                minimoTramosCancelacion, minimoCapacidadesDistintas);
    }
}
