package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.CancelarReservaExterna;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.CancelarReservaExternaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ConsultarDisponibilidadCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ConsultarDisponibilidadCanalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.IntegracionExternaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RecibirReservaExterna;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RecibirReservaExternaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarEventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarEventoCanalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ApartamentoDisponibleResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CancelacionResult;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.AutenticadorCanal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Adaptador de entrada de los canales externos (CU-12 · CU-13 · CU-16): primero autentica el mensaje con la clave del
 * canal (CAN-03 · DEC-73) y luego llama al caso de uso. Si este lo rechaza o falla, deja el mensaje en la bitácora
 * como RECHAZADO o ERROR (BIT-03 · DEC-62); lo que sale bien lo anota el propio caso de uso. Un mensaje sin clave
 * válida no llega a la bitácora: así nadie puede llenarla desde afuera.
 */
@Component
public class ReceptorCanalExterno {

    private static final Logger LOG = LoggerFactory.getLogger(ReceptorCanalExterno.class);
    private static final int MAXIMO_DETALLE = 500;

    private final AutenticadorCanal autenticador;
    private final RecibirReservaExterna recibirReservaExterna;
    private final ConsultarDisponibilidadCanal consultarDisponibilidad;
    private final CancelarReservaExterna cancelarReservaExterna;
    private final RegistrarEventoCanal registrarEvento;

    public ReceptorCanalExterno(AutenticadorCanal autenticador, RecibirReservaExterna recibirReservaExterna,
                                ConsultarDisponibilidadCanal consultarDisponibilidad,
                                CancelarReservaExterna cancelarReservaExterna, RegistrarEventoCanal registrarEvento) {
        this.autenticador = autenticador;
        this.recibirReservaExterna = recibirReservaExterna;
        this.consultarDisponibilidad = consultarDisponibilidad;
        this.cancelarReservaExterna = cancelarReservaExterna;
        this.registrarEvento = registrarEvento;
    }

    /**
     * @param clave la del encabezado del mensaje; nula si no vino
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.CanalNoAutenticadoException si la clave
     *         no es la del canal
     */
    public IntegracionExternaResult recibir(String clave, RecibirReservaExternaCommand comando) {
        autenticador.exigir(comando.canalId(), clave);
        return anotandoRechazos(comando.canalId(), "CREAR_RESERVA", comando.cargaUtil(),
                () -> recibirReservaExterna.ejecutar(comando));
    }

    /** @see #recibir */
    public List<ApartamentoDisponibleResult> consultarDisponibilidad(String clave,
                                                                     ConsultarDisponibilidadCanalCommand comando) {
        autenticador.exigir(comando.canalId(), clave);
        return anotandoRechazos(comando.canalId(), "CONSULTAR_DISPONIBILIDAD", comando.cargaUtil(),
                () -> consultarDisponibilidad.ejecutar(comando));
    }

    /** @see #recibir */
    public CancelacionResult cancelar(String clave, CancelarReservaExternaCommand comando) {
        autenticador.exigir(comando.canalId(), clave);
        return anotandoRechazos(comando.canalId(), "CANCELAR_RESERVA", comando.cargaUtil(),
                () -> cancelarReservaExterna.ejecutar(comando));
    }

    /**
     * Ejecuta la operación y, si la rechaza o falla, la anota y deja pasar la misma excepción, para que el canal reciba
     * su respuesta de error.
     */
    private <T> T anotandoRechazos(String canalId, String operacion, String cargaUtil, Supplier<T> operar) {
        try {
            return operar.get();
        } catch (ReglaDominioException | RecursoNoEncontradoException rechazo) {
            anotar(canalId, operacion, cargaUtil, "RECHAZADO", rechazo.getMessage());
            throw rechazo;
        } catch (RuntimeException error) {
            anotar(canalId, operacion, cargaUtil, "ERROR", error.getClass().getSimpleName());
            throw error;
        }
    }

    /** Un fallo de la bitácora no debe cambiar la respuesta al canal (BIT-06): solo queda en el log. */
    private void anotar(String canalId, String operacion, String cargaUtil, String resultado, String detalle) {
        try {
            registrarEvento.ejecutar(new RegistrarEventoCanalCommand(canalId, operacion, "ENTRANTE", cargaUtil,
                    resultado, recortar(detalle)));
        } catch (RuntimeException fallo) {
            LOG.warn("No se pudo anotar en la bitácora el mensaje {} del canal {}: {}", operacion, canalId,
                    fallo.getMessage());
        }
    }

    private static String recortar(String detalle) {
        String texto = String.valueOf(detalle);
        return texto.substring(0, Math.min(texto.length(), MAXIMO_DETALLE));
    }
}
