package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: ver el detalle de una reserva por su código (CU-49). Que un huésped solo vea las suyas lo controla
 * la seguridad (B3).
 */
@Service
public class ConsultarReserva {

    private final CargadorReserva cargador;

    public ConsultarReserva(CargadorReserva cargador) {
        this.cargador = cargador;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public ReservaResult ejecutar(String codigo) {
        return ReservaResult.de(cargador.reserva(codigo));
    }
}
