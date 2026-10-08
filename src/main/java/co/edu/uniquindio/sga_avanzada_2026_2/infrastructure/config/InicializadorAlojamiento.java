package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.InicializarAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.InicializarAlojamientoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.InicializarAlojamientoCommand.ServicioCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.ParametrosCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaCommand.PenalizacionCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaCommand.TramoCommand;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Al arrancar crea el alojamiento del despliegue con los valores de {@code sga.*} si todavía no existe (ALO-01 ·
 * DEC-53). Se apaga con {@code sga.inicializar=false} (las pruebas lo apagan para no escribir en la BD compartida).
 */
@Component
@ConditionalOnProperty(name = "sga.inicializar", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SgaPropiedades.class)
public class InicializadorAlojamiento implements ApplicationRunner {

    private final InicializarAlojamiento inicializarAlojamiento;
    private final SgaPropiedades propiedades;

    public InicializadorAlojamiento(InicializarAlojamiento inicializarAlojamiento, SgaPropiedades propiedades) {
        this.inicializarAlojamiento = inicializarAlojamiento;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        inicializarAlojamiento.ejecutar(comando(propiedades));
    }

    static InicializarAlojamientoCommand comando(SgaPropiedades sga) {
        SgaPropiedades.Alojamiento a = sga.alojamiento();
        SgaPropiedades.Parametros p = sga.parametros();
        return new InicializarAlojamientoCommand(a.id(), a.nombre(), a.descripcion(), a.ciudad(), a.direccion(),
                a.latitud(), a.longitud(), a.normas(),
                new ParametrosCommand(p.umbralEdadFacturable(), p.horaEntrada(), p.horaSalida(),
                        p.tiempoPreparacionHoras(), p.plazoConfirmacionHoras(), p.horaLimiteNoShow(), p.anticipoPct(),
                        p.minimoMediosPago(), p.minimoServiciosAdicionales(), p.minimoTemporadas(),
                        p.minimoTramosCancelacion(), p.minimoCapacidadesDistintas()),
                a.servicios().stream().map(s -> new ServicioCommand(s.nombre(), s.generaCargo(), s.valor())).toList(),
                a.mediosPago(),
                sga.politica().tramos().stream()
                        .map(t -> new TramoCommand(t.antelacionMinHoras(), penalizacion(t.penalizacion())))
                        .toList(),
                penalizacion(sga.politica().noShow()),
                a.temporadaBase());
    }

    private static PenalizacionCommand penalizacion(SgaPropiedades.Penalizacion p) {
        return new PenalizacionCommand(p.base(), p.porcentaje(), p.montoFijo());
    }
}
