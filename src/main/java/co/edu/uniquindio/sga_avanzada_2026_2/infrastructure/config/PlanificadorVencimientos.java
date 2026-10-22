package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarReservasVencidasUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.VencerReservaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Vence cada minuto las reservas PENDIENTE que superaron su plazo para confirmarse, y así libera sus noches (CU-20 ·
 * RN-21 · DEC-57 · DEC-69). No es un endpoint: corre dentro de la aplicación. Se apaga con
 * {@code sga.planificador.activo=false} (las pruebas lo apagan y lo llaman a mano).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "sga.planificador.activo", havingValue = "true", matchIfMissing = true)
public class PlanificadorVencimientos {

    private static final Logger LOG = LoggerFactory.getLogger(PlanificadorVencimientos.class);

    private final BuscarReservasVencidasUseCase buscarVencidas;
    private final VencerReservaUseCase vencerReserva;
    private final String alojamientoId;

    public PlanificadorVencimientos(BuscarReservasVencidasUseCase buscarVencidas, VencerReservaUseCase vencerReserva,
                                    @Value("${sga.alojamiento.id:ALO-1}") String alojamientoId) {
        this.buscarVencidas = buscarVencidas;
        this.vencerReserva = vencerReserva;
        this.alojamientoId = alojamientoId;
    }

    /**
     * Cada reserva se vence en su propia transacción: si una falla (p. ej. la pagaron justo en ese momento), queda en
     * el log y las demás siguen (DEC-57).
     *
     * @return cuántas venció
     */
    @Scheduled(fixedDelayString = "${sga.planificador.intervalo:PT1M}")
    public int vencerPendientes() {
        int vencidas = 0;
        for (String codigo : buscarVencidas.ejecutar(alojamientoId)) {
            try {
                vencerReserva.ejecutar(codigo);
                vencidas++;
            } catch (RuntimeException e) {
                LOG.warn("No se pudo vencer la reserva {}: {}", codigo, e.getMessage());
            }
        }
        return vencidas;
    }
}
