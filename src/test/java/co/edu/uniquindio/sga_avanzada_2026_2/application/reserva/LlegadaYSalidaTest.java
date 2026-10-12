package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.SalidaOperativaDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ADMIN;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ALOJADO;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.AL_LLEGAR;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.AL_SALIR;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ENTRADA;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.RECEPCION;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.relojEn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Llegada, corrección del registro y salida sobre la reserva de {@link EscenarioEstancia}: del 10 al 13 de octubre
 * por 600.000, con el anticipo de 180.000 pagado.
 */
class LlegadaYSalidaTest {

    private EscenarioEstancia escenario;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioEstancia();
    }

    private RegistrarSalida registrarSalida(Clock reloj) {
        return new RegistrarSalida(escenario.cargador, escenario.reservas, escenario.apartamentos, escenario.folios,
                new SalidaOperativaDomainService(), reloj);
    }

    private CorregirRegistro corregirRegistro() {
        return new CorregirRegistro(escenario.cargador, escenario.reservas, ALOJADO);
    }

    private void ponerApartamentoEn(EstadoOperativo estado) {
        Apartamento actual = escenario.apartamento();
        escenario.apartamentos.guardar(new Apartamento(actual.codigo(), actual.alojamientoId(), actual.nombre(),
                actual.descripcion(), actual.capacidad(), actual.dormitorios(), estado, actual.imagenes(),
                actual.caracteristicas(), actual.bloqueos(), actual.activo()));
    }

    @Test
    @Tag("RN-10")
    @Tag("RN-11")
    @Tag("REG-04")
    void deberiaRegistrarLaLlegadaYOcuparElApartamentoALaVez() {
        String codigo = escenario.reservaConfirmada();

        EstanciaResult estancia = escenario.registrarLlegada(AL_LLEGAR)
                .ejecutar(new RegistrarLlegadaCommand(codigo, RECEPCION));

        assertEquals("EN_CURSO", estancia.estado());
        assertEquals("OCUPADO", estancia.estadoOperativo());
        assertEquals("REG-1", estancia.registros().getFirst().id());
        assertEquals(LocalDateTime.now(AL_LLEGAR), estancia.registros().getFirst().fechaHora());
        assertEquals(RECEPCION, estancia.registros().getFirst().autor());
        assertNull(estancia.salida());
        assertFalse(estancia.folioCerrado());
        assertEquals(0, BigDecimal.valueOf(420_000).compareTo(estancia.saldo()));
        assertEquals(EstadoOperativo.OCUPADO, escenario.apartamento().estadoOperativo());
    }

    @Test
    @Tag("RN-10")
    @Tag("REG-01")
    @Tag("REG-02")
    void noDeberiaRegistrarLaLlegadaSinConfirmarNiAntesDeLaFechaDeEntrada() {
        String pendiente = escenario.reservaPendiente();

        assertThrows(ReglaDominioException.class, () -> escenario.registrarLlegada(AL_LLEGAR)
                .ejecutar(new RegistrarLlegadaCommand(pendiente, RECEPCION)));

        EscenarioEstancia otro = new EscenarioEstancia();
        String confirmada = otro.reservaConfirmada();
        assertThrows(ReglaDominioException.class,
                () -> otro.registrarLlegada(relojEn(ENTRADA.minusDays(1).atTime(18, 0)))
                        .ejecutar(new RegistrarLlegadaCommand(confirmada, RECEPCION)));
        assertEquals(EstadoReserva.CONFIRMADA, otro.reserva(confirmada).estado());
        assertEquals(EstadoOperativo.PREPARADO, otro.apartamento().estadoOperativo());
    }

    @Test
    @Tag("RN-11")
    @Tag("REG-03")
    void noDeberiaEntregarUnApartamentoQueNoEstaPreparado() {
        String codigo = escenario.reservaConfirmada();
        ponerApartamentoEn(EstadoOperativo.EN_PREPARACION);

        assertThrows(ReglaDominioException.class, () -> escenario.registrarLlegada(AL_LLEGAR)
                .ejecutar(new RegistrarLlegadaCommand(codigo, RECEPCION)));
        assertEquals(EstadoReserva.CONFIRMADA, escenario.reserva(codigo).estado());
        assertTrue(escenario.reserva(codigo).registros().isEmpty());
    }

    @Test
    @Tag("REG-06")
    void deberiaCorregirElRegistroConservandoElAnterior() {
        String codigo = escenario.reservaEnCurso();
        LocalDateTime llegadaReal = ENTRADA.atTime(14, 10);

        EstanciaResult estancia = corregirRegistro()
                .ejecutar(new CorregirRegistroCommand(codigo, llegadaReal, "Se digitó mal la hora", RECEPCION));

        assertEquals("EN_CURSO", estancia.estado());
        assertEquals(List.of("REG-1", "REG-2"),
                estancia.registros().stream().map(EstanciaResult.RegistroResult::id).toList());
        assertTrue(estancia.registros().getFirst().anulado());
        assertFalse(estancia.registros().getLast().anulado());
        assertEquals(llegadaReal, estancia.registros().getLast().fechaHora());
        assertEquals("Se digitó mal la hora", estancia.registros().getLast().motivoCorreccion());
        assertEquals("OCUPADO", estancia.estadoOperativo());
    }

    @Test
    @Tag("REG-06")
    void noDeberiaCorregirElRegistroSinMotivoNiConUnaFechaImposible() {
        String codigo = escenario.reservaEnCurso();

        assertThrows(ReglaDominioException.class, () -> corregirRegistro()
                .ejecutar(new CorregirRegistroCommand(codigo, ENTRADA.atTime(14, 0), " ", RECEPCION)));
        assertThrows(ReglaDominioException.class, () -> corregirRegistro().ejecutar(
                new CorregirRegistroCommand(codigo, ENTRADA.minusDays(1).atTime(14, 0), "Error", RECEPCION)));
        assertThrows(ReglaDominioException.class, () -> corregirRegistro().ejecutar(
                new CorregirRegistroCommand(codigo, ENTRADA.plusDays(2).atTime(14, 0), "Error", RECEPCION)));
        assertEquals(1, escenario.reserva(codigo).registros().size());
    }

    @Test
    @Tag("RN-17")
    @Tag("SAL-02")
    void noDeberiaRegistrarLaSalidaConSaldoPendienteSinAutorizacion() {
        String codigo = escenario.reservaEnCurso(); // debe 420.000

        assertThrows(ReglaDominioException.class,
                () -> registrarSalida(AL_SALIR).ejecutar(new RegistrarSalidaCommand(codigo, RECEPCION, null)));
        assertEquals(EstadoReserva.EN_CURSO, escenario.reserva(codigo).estado());
        assertEquals(EstadoOperativo.OCUPADO, escenario.apartamento().estadoOperativo());
        assertFalse(escenario.folio(codigo).cerrado());
    }

    @Test
    @Tag("SAL-02")
    @Tag("SAL-03")
    @Tag("RN-12")
    void deberiaRegistrarLaSalidaCerrarElFolioYLiberarElApartamento() {
        String codigo = escenario.reservaEnCurso();
        escenario.pagar(codigo, 420_000, AL_SALIR);

        EstanciaResult estancia = registrarSalida(AL_SALIR)
                .ejecutar(new RegistrarSalidaCommand(codigo, RECEPCION, null));

        assertEquals("FINALIZADA", estancia.estado());
        assertEquals("PENDIENTE_PREPARACION", estancia.estadoOperativo());
        assertEquals("SAL-1", estancia.salida().id());
        assertEquals(LocalDateTime.now(AL_SALIR), estancia.salida().fechaHora());
        assertTrue(estancia.folioCerrado());
        assertEquals("AL_DIA", estancia.situacionSaldo());
        assertTrue(escenario.folio(codigo).cerrado());
    }

    @Test
    @Tag("SAL-04")
    @Tag("SAL-06")
    @Tag("RN-12")
    void unaSalidaAnticipadaLiberaLasNochesRestantes() {
        String codigo = escenario.reservaEnCurso();
        escenario.pagar(codigo, 420_000, ALOJADO);

        registrarSalida(ALOJADO).ejecutar(new RegistrarSalidaCommand(codigo, RECEPCION, ""));

        BuscarDisponibles buscar = new BuscarDisponibles(escenario.cargador, escenario.apartamentos,
                escenario.reservas, new DisponibilidadDomainService());
        assertEquals(EstadoReserva.FINALIZADA, escenario.reserva(codigo).estado());
        assertEquals(1, buscar.ejecutar(new BuscarDisponiblesCommand("ALO-1", ENTRADA.plusDays(1),
                ENTRADA.plusDays(3), 2)).size());
    }

    @Test
    @Tag("RN-17")
    @Tag("AUTC-01")
    @Tag("AUTC-04")
    void deberiaRegistrarLaSalidaConSaldoSiElAdministradorLaAutoriza() {
        String codigo = escenario.reservaEnCurso();

        EstanciaResult estancia = registrarSalida(AL_SALIR)
                .ejecutar(new RegistrarSalidaCommand(codigo, ADMIN, "Pagará por transferencia el lunes"));

        assertEquals("FINALIZADA", estancia.estado());
        assertTrue(estancia.folioCerrado());
        assertEquals("PENDIENTE", estancia.situacionSaldo());
        assertEquals(0, BigDecimal.valueOf(420_000).compareTo(estancia.saldo()));
        assertEquals("Pagará por transferencia el lunes", escenario.folio(codigo).autorizacion().motivo());
        assertEquals(ADMIN, escenario.folio(codigo).autorizacion().autor().valor());
    }

    @Test
    @Tag("SAL-01")
    void noDeberiaRegistrarLaSalidaDeUnaReservaQueNoHaLlegado() {
        String codigo = escenario.reservaConfirmada();

        assertThrows(ReglaDominioException.class,
                () -> registrarSalida(AL_SALIR).ejecutar(new RegistrarSalidaCommand(codigo, RECEPCION, null)));
        assertFalse(escenario.folio(codigo).cerrado());
    }
}
