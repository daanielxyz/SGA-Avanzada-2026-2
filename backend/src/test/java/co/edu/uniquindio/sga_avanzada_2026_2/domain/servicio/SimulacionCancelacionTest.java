package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Extras;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.SituacionSaldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainServiceTest.folioDe;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainServiceTest.pagar;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainServiceTest.politicaCongelada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.APT;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.parametros;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DEC-91. Reserva del 10 al 12 de diciembre por 200.000 con 50.000 de extras, y 75.000 pagados; la política retiene
 * el 100 % a menos de 24 h, el 50 % de 24 a 72 h y nada desde 72 h.
 */
class SimulacionCancelacionTest {

    private final CancelacionDomainService servicio = new CancelacionDomainService();

    private static Reserva conExtras(Reserva reserva) {
        reserva.declararExtras(1, 0, 1, new Extras(Dinero.de(20_000), Dinero.de(30_000)));
        return reserva;
    }

    private static Folio folioConPago(Reserva reserva) {
        Folio folio = folioDe(reserva);
        folio.cargarExtras(reserva.extras(), dia(1));
        pagar(folio, 75_000);
        return folio;
    }

    private SimulacionCancelacion simular(Reserva reserva, Folio folio, LocalDateTime ahora) {
        return servicio.simularCancelacion(reserva, folio, politicaCongelada(), parametros(3), ahora);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 8, 9, 10})
    @Tag("RN-13")
    @Tag("DEC-91")
    void simularDaLoMismoQueCancelarEnCadaTramo(int diaDeCancelacion) {
        LocalDateTime ahora = dia(diaDeCancelacion).atTime(10, 0);
        Reserva reserva = conExtras(confirmada("RES-2026-00001", 10, 12));
        Folio folio = folioConPago(reserva);

        SimulacionCancelacion simulacion = simular(reserva, folio, ahora);
        Dinero retenido = servicio.procesarCancelacion(reserva, folio, politicaCongelada(), parametros(3), ahora);

        assertEquals(retenido, simulacion.retenido());
        assertEquals(folio.saldo(), simulacion.saldo());
    }

    @Test
    @Tag("DEC-91")
    void simularNoCambiaNiLaReservaNiElFolio() {
        Reserva reserva = conExtras(confirmada("RES-2026-00001", 10, 12));
        Folio folio = folioConPago(reserva);
        Saldo antes = folio.saldo();

        SimulacionCancelacion simulacion = simular(reserva, folio, dia(1).atTime(10, 0)); // sin retención

        assertEquals(Dinero.CERO, simulacion.retenido());
        assertEquals(new Saldo(Dinero.de(75_000), SituacionSaldo.A_FAVOR), simulacion.saldo());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
        assertEquals(antes, folio.saldo());
    }

    @Test
    @Tag("RN-08")
    @Tag("DEC-91")
    void unaReservaQueYaNoSePuedeCancelarNoSeSimula() {
        Reserva cancelada = reserva("RES-2026-00001", APT, 10, 12, EstadoReserva.CANCELADA);

        assertThrows(ReglaDominioException.class, () -> simular(cancelada, folioDe(cancelada), dia(1).atTime(10, 0)));
    }

    @Test
    @Tag("CAR-07")
    @Tag("DEC-91")
    void conElFolioCerradoNoSeSimula() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);
        pagar(folio, 200_000);
        folio.cerrar();

        assertThrows(ReglaDominioException.class, () -> simular(reserva, folio, dia(1).atTime(10, 0)));
    }
}
