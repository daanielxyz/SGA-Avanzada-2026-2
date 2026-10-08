package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * Salida de los casos de uso de Alojamiento (DEC-31): datos, parámetros y catálogos, nunca la entidad.
 */
public record AlojamientoResult(String id, String nombre, String descripcion, String ciudad, String direccion,
                                double latitud, double longitud, String normas, ParametrosResult parametros,
                                List<ServicioResult> serviciosAdicionales, List<String> mediosPago) {

    public record ParametrosResult(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                                   long tiempoPreparacionHoras, long plazoConfirmacionHoras,
                                   LocalTime horaLimiteNoShow, int anticipoPct, int minimoMediosPago,
                                   int minimoServiciosAdicionales, int minimoTemporadas, int minimoTramosCancelacion,
                                   int minimoCapacidadesDistintas) {
    }

    public record ServicioResult(String id, String nombre, boolean generaCargo, BigDecimal valor, boolean activo) {
    }

    public static AlojamientoResult de(Alojamiento alojamiento) {
        ParametrosAlojamiento p = alojamiento.parametros();
        return new AlojamientoResult(
                alojamiento.id().valor(),
                alojamiento.nombre(),
                alojamiento.descripcion(),
                alojamiento.ciudad(),
                alojamiento.direccion(),
                alojamiento.ubicacion().latitud(),
                alojamiento.ubicacion().longitud(),
                alojamiento.normas(),
                new ParametrosResult(p.umbralEdadFacturable(), p.horaEntrada(), p.horaSalida(),
                        p.tiempoPreparacion().toHours(), p.plazoConfirmacion().toHours(), p.horaLimiteNoShow(),
                        p.anticipo().valor(), p.minimoMediosPago(), p.minimoServiciosAdicionales(),
                        p.minimoTemporadas(), p.minimoTramosCancelacion(), p.minimoCapacidadesDistintas()),
                alojamiento.serviciosAdicionales().stream()
                        .map(s -> new ServicioResult(s.id().valor(), s.nombre(), s.generaCargo(), s.valor().monto(),
                                s.activo()))
                        .toList(),
                alojamiento.mediosPago().stream().map(MedioPago::nombre).toList());
    }
}
