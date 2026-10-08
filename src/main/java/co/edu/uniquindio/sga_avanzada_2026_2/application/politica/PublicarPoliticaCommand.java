package co.edu.uniquindio.sga_avanzada_2026_2.application.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Entrada de {@link PublicarPolitica} (CU-35): los tramos por horas y la penalización del no-show que define el
 * administrador del alojamiento (DEC-41).
 */
public record PublicarPoliticaCommand(String alojamientoId, List<TramoCommand> tramos,
                                      PenalizacionCommand penalizacionNoShow) {

    public record TramoCommand(int antelacionMinHoras, PenalizacionCommand penalizacion) {

        public TramoCancelacion aDominio() {
            return new TramoCancelacion(antelacionMinHoras, penalizacion.aDominio());
        }
    }

    /**
     * Porcentaje o monto fijo, no ambos (POL-03 · DEC-41).
     *
     * @param base nombre de {@link BaseRetencion}
     */
    public record PenalizacionCommand(String base, Integer porcentaje, BigDecimal montoFijo) {

        public Penalizacion aDominio() {
            return new Penalizacion(BaseRetencion.valueOf(base),
                    Optional.ofNullable(porcentaje).map(Porcentaje::new).orElse(null),
                    Optional.ofNullable(montoFijo).map(Dinero::new).orElse(null));
        }
    }

    public List<TramoCancelacion> tramosDominio() {
        return tramos.stream().map(TramoCommand::aDominio).toList();
    }
}
