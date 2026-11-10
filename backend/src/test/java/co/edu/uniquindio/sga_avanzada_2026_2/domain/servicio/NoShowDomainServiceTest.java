package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Extras;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.SituacionSaldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainServiceTest.folioDe;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainServiceTest.pagar;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainServiceTest.politicaCongelada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainServiceTest.porcentaje;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.APT;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.parametros;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reserva del 10 al 12 de diciembre por 200.000; hora límite de no-show 22:00, anticipo exigido 30 % (60.000).
 */
class NoShowDomainServiceTest {

    private static final PoliticaCancelacion RETIENE_EL_ANTICIPO =
            politicaCongelada(porcentaje(BaseRetencion.ANTICIPO_EXIGIDO, 100));

    private static final UsuarioId AUTOR = new UsuarioId("USR-1");

    private final NoShowDomainService servicio = new NoShowDomainService();

    @Test
    @Tag("POL-06")
    @Tag("RN-13")
    void deberiaLiquidarLaConsecuenciaDelNoShowDeLaPoliticaCongelada() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);
        pagar(folio, 60_000);

        Dinero retenido = servicio.declararNoShow(reserva, folio, RETIENE_EL_ANTICIPO, parametros(3),
                dia(10).atTime(22, 0), AUTOR);

        assertEquals(Dinero.de(60_000), retenido);
        assertEquals(EstadoReserva.NO_SHOW, reserva.estado());
        assertEquals(new Saldo(Dinero.CERO, SituacionSaldo.AL_DIA), folio.saldo());
        assertEquals(AUTOR, reserva.declaracionNoShow().autor());
    }

    @Test
    @Tag("RES-16")
    void noDeberiaTocarElFolioSiAunNoLlegaLaHoraLimite() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);

        assertThrows(ReglaDominioException.class, () -> servicio.declararNoShow(reserva, folio,
                RETIENE_EL_ANTICIPO, parametros(3), dia(10).atTime(21, 59), AUTOR));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
        assertEquals(1, folio.cargos().size());
    }

    private static Reserva conExtras() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        reserva.declararExtras(1, 0, 1, new Extras(Dinero.de(20_000), Dinero.de(30_000)));
        return reserva;
    }

    private static Folio folioConPago(Reserva reserva) {
        Folio folio = folioDe(reserva);
        folio.cargarExtras(reserva.extras(), dia(1));
        pagar(folio, 75_000);
        return folio;
    }

    private SimulacionCancelacion simular(Reserva reserva, Folio folio, int hora) {
        return servicio.simularNoShow(reserva, folio, RETIENE_EL_ANTICIPO, parametros(3), dia(10).atTime(hora, 0));
    }

    @Test
    @Tag("POL-06")
    @Tag("DEC-102")
    void simularDaLoMismoQueDeclarar() {
        Reserva reserva = conExtras();
        Folio folio = folioConPago(reserva);

        SimulacionCancelacion simulacion = simular(reserva, folio, 22);
        Dinero retenido = servicio.declararNoShow(reserva, folio, RETIENE_EL_ANTICIPO, parametros(3),
                dia(10).atTime(22, 0), AUTOR);

        assertEquals(retenido, simulacion.retenido());
        assertEquals(folio.saldo(), simulacion.saldo());
    }

    @Test
    @Tag("DEC-102")
    void simularNoCambiaNiLaReservaNiElFolio() {
        Reserva reserva = conExtras();
        Folio folio = folioConPago(reserva);
        Saldo antes = folio.saldo();
        int cargos = folio.cargos().size();

        SimulacionCancelacion simulacion = simular(reserva, folio, 23);

        assertEquals(Dinero.de(60_000), simulacion.retenido());
        assertEquals(new Saldo(Dinero.de(15_000), SituacionSaldo.A_FAVOR), simulacion.saldo());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
        assertEquals(antes, folio.saldo());
        assertEquals(cargos, folio.cargos().size());
    }

    @Test
    @Tag("RES-16")
    @Tag("DEC-102")
    void antesDeLaHoraLimiteNoSeSimula() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);

        assertThrows(ReglaDominioException.class, () -> simular(reserva, folioDe(reserva), 21));
    }

    @Test
    @Tag("RN-08")
    @Tag("DEC-102")
    void unaReservaQueNoEstaConfirmadaNoSeSimula() {
        Reserva pendiente = reserva("RES-2026-00001", APT, 10, 12, EstadoReserva.PENDIENTE);

        assertThrows(ReglaDominioException.class, () -> simular(pendiente, folioDe(pendiente), 23));
    }

    @Test
    @Tag("CAR-07")
    @Tag("DEC-102")
    void conElFolioCerradoNoSeSimula() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);
        pagar(folio, 200_000);
        folio.cerrar();

        assertThrows(ReglaDominioException.class, () -> simular(reserva, folio, 23));
    }
}
