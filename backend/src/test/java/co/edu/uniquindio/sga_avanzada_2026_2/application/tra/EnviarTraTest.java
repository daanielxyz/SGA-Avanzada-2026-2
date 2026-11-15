package co.edu.uniquindio.sga_avanzada_2026_2.application.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EstadoEnvioTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.HuespedTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.ReporteTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.ReporteTraDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.TraNoDisponibleException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.TransmisorTra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El envío del TRA de punta a punta con repositorios en memoria y un Ministerio de mentiras (DEC-130): la llegada lo
 * deja pendiente, sale cuando los datos están completos, un fallo no repite lo que ya entró y sin RNT no sale nada.
 */
@Tag("DEC-130")
class EnviarTraTest {

    /** Anota lo que «recibe» el Ministerio; puede fallar en el acompañante número {@code fallarEn}. */
    static final class MinisterioDePrueba implements TransmisorTra {

        final List<String> recibidos = new ArrayList<>();
        int fallarEn = -1;
        ReporteTra ultimo;

        @Override
        public String enviarPrincipal(ReporteTra reporte) {
            ultimo = reporte;
            recibidos.add("principal " + reporte.principal().nombres());
            return "19";
        }

        @Override
        public void enviarAcompanante(ReporteTra reporte, HuespedTra acompanante, String codigoPrincipal) {
            if (fallarEn == recibidos.size()) {
                throw new TraNoDisponibleException("El Ministerio respondió 500");
            }
            recibidos.add("acompañante " + acompanante.nombres() + " de " + codigoPrincipal);
        }
    }

    private EscenarioEstancia escenario;
    private MinisterioDePrueba ministerio;
    private EnviarTraUseCase enviar;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioEstancia();
        Alojamiento alojamiento = escenario.alojamientos.buscarPorId(new AlojamientoId("ALO-1")).orElseThrow();
        alojamiento.registrarRnt("123456");
        escenario.alojamientos.guardar(alojamiento);
        ministerio = new MinisterioDePrueba();
        enviar = new EnviarTraUseCase(escenario.cargador, escenario.enviosTra, new ReporteTraDomainService(),
                ministerio, "Apartamento", EscenarioEstancia.ALOJADO);
    }

    private EstadoEnvioTra estado(String codigo) {
        return escenario.enviosTra.buscarPorReserva(new ReservaId(codigo)).orElseThrow().estado();
    }

    @Test
    void laLlegadaDejaElEnvioPendiente() {
        String codigo = escenario.reservaConfirmada();
        escenario.registrarLlegada(EscenarioEstancia.AL_LLEGAR)
                .ejecutar(new RegistrarLlegadaCommand(codigo, EscenarioEstancia.RECEPCION, "Cuenta por cobrar"));

        assertEquals(EstadoEnvioTra.PENDIENTE, estado(codigo));
        assertEquals(List.of(new ReservaId(codigo)), escenario.enviosTra.buscarParaEnviar(12));
    }

    @Test
    void conLosDatosCompletosSaleElPrincipalYDespuesSusAcompanantes() {
        String codigo = escenario.reservaEnCurso();

        EnvioTraResult resultado = enviar.ejecutar(codigo);

        assertEquals("ENVIADO", resultado.estado());
        assertEquals("19", resultado.codigoMinisterio());
        assertEquals(1, resultado.acompanantesEnviados());
        assertEquals(List.of("principal Ana", "acompañante Luis de 19"), ministerio.recibidos);
        ReporteTra reporte = ministerio.ultimo;
        assertEquals("C.C", reporte.principal().tipoDocumento());
        assertEquals("Ríos", reporte.principal().apellidos());
        assertEquals("Turismo y vacaciones", reporte.motivo());
        assertEquals("APT-101", reporte.numeroHabitacion());
        assertEquals("Apartamento", reporte.tipoAcomodacion());
        assertEquals("Puerta al Sol", reporte.nombreEstablecimiento());
        assertEquals("123456", reporte.rnt());
        assertEquals(1, reporte.numeroAcompanantes());
    }

    @Test
    void conDatosIncompletosNoSaleNadaYSeDicePorQue() {
        String codigo = escenario.reservaConfirmada();
        escenario.registrarLlegada(EscenarioEstancia.AL_LLEGAR)
                .ejecutar(new RegistrarLlegadaCommand(codigo, EscenarioEstancia.RECEPCION, "Cuenta por cobrar"));

        ReglaDominioException rechazo = assertThrows(ReglaDominioException.class, () -> enviar.ejecutar(codigo));

        assertTrue(rechazo.getMessage().contains("están todos sus datos"));
        assertTrue(ministerio.recibidos.isEmpty());
        assertEquals(EstadoEnvioTra.PENDIENTE, estado(codigo));
    }

    @Test
    void unaReservaQueNoHaLlegadoNoSeReporta() {
        String codigo = escenario.reservaConfirmada();
        escenario.completarDatosTra(codigo);

        assertThrows(ReglaDominioException.class, () -> enviar.ejecutar(codigo));
        assertTrue(ministerio.recibidos.isEmpty());
    }

    @Test
    void siFallaUnAcompananteElReintentoNoRepiteAlPrincipal() {
        String codigo = escenario.reservaEnCurso();
        ministerio.fallarEn = 1;

        EnvioTraResult fallido = enviar.ejecutar(codigo);

        assertEquals("CON_ERROR", fallido.estado());
        assertEquals("El Ministerio respondió 500", fallido.ultimoError());
        assertEquals("19", fallido.codigoMinisterio());
        assertEquals(0, fallido.acompanantesEnviados());

        ministerio.fallarEn = -1;
        EnvioTraResult reintento = enviar.ejecutar(codigo);

        assertEquals("ENVIADO", reintento.estado());
        assertNull(reintento.ultimoError());
        assertEquals(2, reintento.intentos());
        assertEquals(List.of("principal Ana", "acompañante Luis de 19"), ministerio.recibidos);
    }

    @Test
    void sinRntNoSaleNadaYQuedaConError() {
        Alojamiento alojamiento = escenario.alojamientos.buscarPorId(new AlojamientoId("ALO-1")).orElseThrow();
        alojamiento.registrarRnt("");
        escenario.alojamientos.guardar(alojamiento);
        String codigo = escenario.reservaEnCurso();

        EnvioTraResult resultado = enviar.ejecutar(codigo);

        assertEquals("CON_ERROR", resultado.estado());
        assertTrue(resultado.ultimoError().contains("Falta el RNT"));
        assertTrue(ministerio.recibidos.isEmpty());
    }

    @Test
    void loYaEnviadoNoSeVuelveAEnviar() {
        String codigo = escenario.reservaEnCurso();
        enviar.ejecutar(codigo);

        ReglaDominioException rechazo = assertThrows(ReglaDominioException.class, () -> enviar.ejecutar(codigo));

        assertTrue(rechazo.getMessage().contains("ya se envió"));
        assertEquals(2, ministerio.recibidos.size());
        assertTrue(escenario.enviosTra.buscarParaEnviar(12).isEmpty());
    }

    @Test
    void elPlanificadorDejaDeReintentarSoloDespuesDelMaximo() {
        Alojamiento alojamiento = escenario.alojamientos.buscarPorId(new AlojamientoId("ALO-1")).orElseThrow();
        alojamiento.registrarRnt(null);
        escenario.alojamientos.guardar(alojamiento);
        String codigo = escenario.reservaEnCurso();
        enviar.ejecutar(codigo);
        enviar.ejecutar(codigo);

        assertEquals(List.of(new ReservaId(codigo)), escenario.enviosTra.buscarParaEnviar(3));
        assertTrue(escenario.enviosTra.buscarParaEnviar(2).isEmpty());
    }
}
