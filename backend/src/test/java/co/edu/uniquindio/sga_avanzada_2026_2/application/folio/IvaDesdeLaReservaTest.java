package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConfirmarReservaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.IndicarHoraLlegadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.IndicarHoraLlegadaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ImpuestoAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Cargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.TipoCargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El IVA desde la reserva (DEC-134): si el alojamiento lo cobra, la reserva nace con el IVA congelado y su cargo en el
 * folio, el anticipo se calcula sobre el total con IVA, y la llegada lo deja como estaba si quien llega paga.
 */
@Tag("DEC-134")
class IvaDesdeLaReservaTest {

    private final EscenarioEstancia escenario = new EscenarioEstancia();

    private void cobrarIva(boolean cobra) {
        Alojamiento alojamiento = escenario.alojamientos.buscarPorId(new AlojamientoId("ALO-1")).orElseThrow();
        alojamiento.cambiarImpuesto(new ImpuestoAlojamiento(cobra, new Porcentaje(19)));
        escenario.alojamientos.guardar(alojamiento);
    }

    private List<Cargo> ivas(String codigo) {
        return escenario.folio(codigo).cargos().stream().filter(c -> c.tipo() == TipoCargo.IMPUESTO).toList();
    }

    private void confirmarPagando(String codigo, long monto) {
        new IndicarHoraLlegadaUseCase(escenario.cargador, escenario.reservas)
                .ejecutar(new IndicarHoraLlegadaCommand(codigo, LocalTime.of(16, 0)));
        escenario.pagar(codigo, monto, EscenarioEstancia.AL_CREAR);
        new ConfirmarReservaUseCase(escenario.cargador, escenario.reservas, RepositoriosEnMemoria.RELOJ).ejecutar(codigo);
    }

    @Test
    void conElIvaEncendidoLaReservaNaceConSuIvaYSuCargo() {
        cobrarIva(true);
        String codigo = escenario.reservaPendiente();

        Reserva reserva = escenario.reserva(codigo);
        Dinero iva = reserva.valorTotal().porcentaje(new Porcentaje(19));
        assertEquals(iva, reserva.iva());
        assertEquals(1, ivas(codigo).size());
        assertEquals(iva, ivas(codigo).getFirst().valor());
        assertEquals("IVA del alojamiento (19 %)", ivas(codigo).getFirst().concepto());
    }

    @Test
    @Tag("DEC-141")
    void conLasTarifasConIvaElTotalEsElDeLasNochesYElFolioCargaElAlojamientoSinEl() {
        Alojamiento alojamiento = escenario.alojamientos.buscarPorId(new AlojamientoId("ALO-1")).orElseThrow();
        alojamiento.cambiarImpuesto(new ImpuestoAlojamiento(true, new Porcentaje(19), true));
        escenario.alojamientos.guardar(alojamiento);
        String codigo = escenario.reservaPendiente();

        Reserva reserva = escenario.reserva(codigo);
        ReservaResult resultado = ReservaResult.de(reserva);
        assertEquals(reserva.valorTotal(), reserva.valorEstancia());
        assertEquals(reserva.valorAlojamiento(), escenario.folio(codigo).cargos().stream()
                .filter(c -> c.tipo() == TipoCargo.HOSPEDAJE).findFirst().orElseThrow().valor());
        assertEquals(reserva.iva(), ivas(codigo).getFirst().valor());
        assertTrue(resultado.valor().ivaIncluido());
        assertEquals(reserva.valorAlojamiento().monto(), resultado.valor().alojamiento());
        assertEquals(reserva.valorTotal().monto(), resultado.valor().totalConIva());
    }

    @Test
    void laRespuestaTraeLaBaseElIvaYElTotalConIva() {
        cobrarIva(true);
        Reserva reserva = escenario.reserva(escenario.reservaPendiente());

        ReservaResult resultado = ReservaResult.de(reserva);
        assertEquals(reserva.valorTotal().monto(), resultado.valor().total());
        assertEquals(19, resultado.valor().ivaPorcentaje());
        assertEquals(reserva.iva().monto(), resultado.valor().iva());
        assertEquals(reserva.valorEstancia().monto(), resultado.valor().totalConIva());
    }

    @Test
    void elAnticipoSinElIvaYaNoAlcanzaParaConfirmar() {
        cobrarIva(true);
        String codigo = escenario.reservaPendiente();
        Reserva reserva = escenario.reserva(codigo);
        // 180.000 era el 30 % sin IVA; con IVA el anticipo es mayor
        assertTrue(Dinero.de(180_000).esMenorQue(reserva.anticipoExigido(new Porcentaje(30))));

        assertThrows(ReglaDominioException.class, () -> confirmarPagando(codigo, 180_000));
    }

    @Test
    void conElAnticipoConIvaSeConfirmaYLaLlegadaConCedulaNoCambiaElIva() {
        cobrarIva(true);
        String codigo = escenario.reservaPendiente();
        long anticipo = escenario.reserva(codigo).anticipoExigido(new Porcentaje(30)).monto().longValueExact();
        confirmarPagando(codigo, anticipo);

        escenario.registrarLlegada(EscenarioEstancia.AL_LLEGAR)
                .ejecutar(new RegistrarLlegadaCommand(codigo, EscenarioEstancia.RECEPCION, "Cuenta por cobrar"));

        assertEquals(1, ivas(codigo).size());
        assertTrue(escenario.folio(codigo).tieneIvaVigente());
    }

    @Test
    void conElIvaApagadoNoHayIvaNiAlCrearNiEnLaLlegada() {
        String codigo = escenario.reservaConfirmada();
        escenario.registrarLlegada(EscenarioEstancia.AL_LLEGAR)
                .ejecutar(new RegistrarLlegadaCommand(codigo, EscenarioEstancia.RECEPCION, "Cuenta por cobrar"));

        assertTrue(ivas(codigo).isEmpty());
        assertEquals(Dinero.CERO, escenario.reserva(codigo).iva());
    }

    @Test
    void unaReservaCreadaAntesDeEncenderloSigueSinIva() {
        String codigo = escenario.reservaConfirmada();
        cobrarIva(true);

        escenario.registrarLlegada(EscenarioEstancia.AL_LLEGAR)
                .ejecutar(new RegistrarLlegadaCommand(codigo, EscenarioEstancia.RECEPCION, "Cuenta por cobrar"));

        assertTrue(ivas(codigo).isEmpty());
    }
}
