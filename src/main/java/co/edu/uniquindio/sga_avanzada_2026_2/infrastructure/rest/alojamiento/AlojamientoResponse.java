package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.AlojamientoResult;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * JSON del alojamiento hacia el cliente (DEC-31): sus datos, sus parámetros, su catálogo de servicios y sus medios
 * de pago.
 */
public record AlojamientoResponse(String id, String nombre, String descripcion, String ciudad, String direccion,
                                  double latitud, double longitud, String normas, ParametrosResponse parametros,
                                  List<ServicioResponse> serviciosAdicionales, List<String> mediosPago) {

    public record ParametrosResponse(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                                     long tiempoPreparacionHoras, long plazoConfirmacionHoras,
                                     LocalTime horaLimiteNoShow, int anticipoPct, int minimoMediosPago,
                                     int minimoServiciosAdicionales, int minimoTemporadas,
                                     int minimoTramosCancelacion, int minimoCapacidadesDistintas) {
    }

    public record ServicioResponse(String id, String nombre, boolean generaCargo, BigDecimal valor, boolean activo) {
    }

    static AlojamientoResponse de(AlojamientoResult r) {
        AlojamientoResult.ParametrosResult p = r.parametros();
        return new AlojamientoResponse(r.id(), r.nombre(), r.descripcion(), r.ciudad(), r.direccion(), r.latitud(),
                r.longitud(), r.normas(),
                new ParametrosResponse(p.umbralEdadFacturable(), p.horaEntrada(), p.horaSalida(),
                        p.tiempoPreparacionHoras(), p.plazoConfirmacionHoras(), p.horaLimiteNoShow(),
                        p.anticipoPct(), p.minimoMediosPago(), p.minimoServiciosAdicionales(), p.minimoTemporadas(),
                        p.minimoTramosCancelacion(), p.minimoCapacidadesDistintas()),
                r.serviciosAdicionales().stream()
                        .map(s -> new ServicioResponse(s.id(), s.nombre(), s.generaCargo(), s.valor(), s.activo()))
                        .toList(),
                r.mediosPago());
    }
}
