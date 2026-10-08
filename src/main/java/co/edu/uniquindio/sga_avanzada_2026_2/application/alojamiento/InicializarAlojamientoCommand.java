package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaCommand.PenalizacionCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaCommand.TramoCommand;

import java.math.BigDecimal;
import java.util.List;

/**
 * Entrada de {@link InicializarAlojamiento}: los valores iniciales del despliegue, que salen de {@code sga.*}
 * (DEC-25). Después el administrador los edita desde la aplicación.
 */
public record InicializarAlojamientoCommand(String id, String nombre, String descripcion, String ciudad,
                                            String direccion, double latitud, double longitud, String normas,
                                            ParametrosCommand parametros, List<ServicioCommand> servicios,
                                            List<String> mediosPago, List<TramoCommand> tramosCancelacion,
                                            PenalizacionCommand penalizacionNoShow, String nombreTemporadaBase) {

    public record ServicioCommand(String nombre, boolean generaCargo, BigDecimal valor) {
    }
}
