package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * Valores iniciales {@code sga.*} del despliegue (DEC-25 · DEC-53): solo se leen la primera vez que arranca, para
 * crear el alojamiento; después el administrador los cambia desde la aplicación.
 */
@ConfigurationProperties("sga")
public record SgaPropiedades(Alojamiento alojamiento, Parametros parametros, Politica politica) {

    public record Alojamiento(String id, String nombre, String descripcion, String ciudad, String direccion,
                              double latitud, double longitud, String normas, List<String> mediosPago,
                              List<Servicio> servicios, String temporadaBase) {
    }

    public record Servicio(String nombre, boolean generaCargo, BigDecimal valor) {
    }

    public record Parametros(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                             int tiempoPreparacionHoras, int plazoConfirmacionHoras, LocalTime horaLimiteNoShow,
                             int anticipoPct, int minimoMediosPago, int minimoServiciosAdicionales,
                             int minimoTemporadas, int minimoTramosCancelacion, int minimoCapacidadesDistintas) {
    }

    public record Politica(List<Tramo> tramos, Penalizacion noShow) {
    }

    public record Tramo(int antelacionMinHoras, Penalizacion penalizacion) {
    }

    /** Porcentaje o monto fijo, no ambos (DEC-41). */
    public record Penalizacion(String base, Integer porcentaje, BigDecimal montoFijo) {
    }
}
