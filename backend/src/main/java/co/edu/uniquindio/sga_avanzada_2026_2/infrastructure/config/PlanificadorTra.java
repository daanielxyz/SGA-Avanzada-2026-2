package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tra.BuscarEnviosTraPendientesUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tra.EnviarTraUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Envía cada 5 minutos el TRA de las reservas pendientes cuyos datos ya están completos (DEC-130). No es un endpoint:
 * corre dentro de la aplicación, con el mismo interruptor que el de los vencimientos ({@code sga.planificador.activo});
 * las pruebas lo apagan y lo llaman a mano.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "sga.planificador.activo", havingValue = "true", matchIfMissing = true)
public class PlanificadorTra {

    private static final Logger LOG = LoggerFactory.getLogger(PlanificadorTra.class);

    private final BuscarEnviosTraPendientesUseCase buscarPendientes;
    private final EnviarTraUseCase enviarTra;

    public PlanificadorTra(BuscarEnviosTraPendientesUseCase buscarPendientes, EnviarTraUseCase enviarTra) {
        this.buscarPendientes = buscarPendientes;
        this.enviarTra = enviarTra;
    }

    /**
     * Cada reserva va por separado: si a una le faltan datos, se queda pendiente para la siguiente vuelta, y si otra
     * falla, queda en el log y las demás siguen.
     *
     * @return cuántas intentó enviar
     */
    @Scheduled(fixedDelayString = "${sga.tra.intervalo:PT5M}", initialDelayString = "${sga.tra.intervalo:PT5M}")
    public int enviarPendientes() {
        int intentadas = 0;
        for (String codigo : buscarPendientes.ejecutar()) {
            try {
                enviarTra.ejecutar(codigo);
                intentadas++;
            } catch (ReglaDominioException todaviaNo) {
                // le faltan datos o el grupo aún no ha llegado: se espera a la siguiente vuelta
            } catch (RuntimeException e) {
                LOG.warn("No se pudo enviar el TRA de la reserva {}: {}", codigo, e.getMessage());
            }
        }
        return intentadas;
    }
}
