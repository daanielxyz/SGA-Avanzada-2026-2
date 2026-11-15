package co.edu.uniquindio.sga_avanzada_2026_2.application.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EnvioTraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: en qué va el envío del TRA de una reserva (DEC-130). */
@Service
public class ConsultarEnvioTraUseCase {

    private final EnvioTraRepository envios;

    public ConsultarEnvioTraUseCase(EnvioTraRepository envios) {
        this.envios = envios;
    }

    /**
     * @throws RecursoNoEncontradoException si la reserva no tiene envío: el grupo todavía no ha llegado
     */
    @Transactional(readOnly = true)
    public EnvioTraResult ejecutar(String codigoReserva) {
        ReservaId reserva = new ReservaId(codigoReserva);
        return envios.buscarPorReserva(reserva).map(EnvioTraResult::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("el envío del TRA de la reserva", reserva.valor()));
    }
}
