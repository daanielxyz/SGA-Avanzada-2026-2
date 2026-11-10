package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.CobroParqueadero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ModoPagoEnLinea;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Extras;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.SituacionSaldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DEC-89. Estancia del 10 al 12 de diciembre por 200.000, con 50.000 de extras: 20.000 de parqueadero y 30.000 de
 * mascotas. El anticipo del 30 % es entonces de 75.000, y no de 60.000.
 */
class ExtrasDeReservaTest {

    private static final Extras EXTRAS = new Extras(Dinero.de(20_000), Dinero.de(30_000));

    private static ParametrosAlojamiento conCobro(CobroParqueadero cobro) {
        return new ParametrosAlojamiento(12, LocalTime.of(15, 0), LocalTime.of(11, 0), Duration.ofHours(3),
                Duration.ofHours(24), LocalTime.of(22, 0), new Porcentaje(30), 2, 1, 2, 2, 2, ModoPagoEnLinea.ANTICIPO,
                Duration.ofMinutes(30), 8, true, 2, Dinero.de(30_000), cobro, Dinero.de(15_000), Dinero.de(8_000));
    }

    private static Reserva conExtras(Reserva reserva) {
        reserva.declararExtras(1, 0, 1, EXTRAS);
        return reserva;
    }

    private static Folio folioConExtras(Reserva reserva) {
        Folio folio = folioDe(reserva);
        folio.cargarExtras(reserva.extras(), dia(1));
        return folio;
    }

    @Test
    @Tag("DEC-89")
    void elParqueaderoPorNocheMultiplicaCadaVehiculoPorLasNoches() {
        Extras extras = conCobro(CobroParqueadero.POR_NOCHE).calcularExtras(2, 1, 1, 3);

        assertEquals(Dinero.de(114_000), extras.parqueadero()); // (2 × 15.000 + 8.000) × 3 noches
        assertEquals(Dinero.de(30_000), extras.mascotas());
        assertEquals(Dinero.de(144_000), extras.total());
    }

    @Test
    @Tag("DEC-89")
    void elParqueaderoPorEstadiaNoDependeDeLasNoches() {
        Extras extras = conCobro(CobroParqueadero.POR_ESTADIA).calcularExtras(2, 1, 0, 3);

        assertEquals(Dinero.de(38_000), extras.parqueadero()); // 2 × 15.000 + 8.000
    }

    @Test
    @Tag("DEC-89")
    void conPrecioEnCeroElHotelNoCobraYElFolioNoLlevaCargo() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);

        folio.cargarExtras(parametros(3).calcularExtras(1, 1, 0, 2), dia(1));

        assertEquals(new Saldo(Dinero.de(200_000), SituacionSaldo.PENDIENTE), folio.saldo());
    }

    @Test
    @Tag("DEC-89")
    void faltarElCobroDelParqueaderoSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> conCobro(null));
    }

    @Test
    @Tag("RES-15")
    @Tag("DEC-89")
    void elAnticipoExigidoEsElMismoQueSeCobraAlConfirmar() {
        Reserva reserva = conExtras(reserva("RES-2026-00001", APT, 10, 12, EstadoReserva.PENDIENTE));

        assertEquals(Dinero.de(75_000), reserva.anticipoExigido(new Porcentaje(30)));
        assertEquals(Dinero.CERO, reserva.anticipoExigido(new Porcentaje(0)));
    }

    @Test
    @Tag("RES-15")
    @Tag("DEC-89")
    void elAnticipoParaConfirmarSeCalculaConLosExtras() {
        Reserva reserva = conExtras(reserva("RES-2026-00001", APT, 10, 12, EstadoReserva.PENDIENTE));
        reserva.indicarHoraEstimadaLlegada(LocalTime.of(15, 0));

        assertThrows(ReglaDominioException.class, () -> reserva.confirmar(new Porcentaje(30), Dinero.de(60_000)));

        reserva.confirmar(new Porcentaje(30), Dinero.de(75_000));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
    }

    @Test
    @Tag("DEC-69")
    @Tag("DEC-89")
    void elPagoEnLineaCobraElAnticipoConLosExtras() {
        assertEquals(Dinero.de(75_000), ModoPagoEnLinea.ANTICIPO.exigido(Dinero.de(250_000), new Porcentaje(30)));
        assertEquals(Dinero.de(250_000), ModoPagoEnLinea.TOTAL.exigido(Dinero.de(250_000), new Porcentaje(30)));
    }

    @Test
    @Tag("RN-13")
    @Tag("DEC-89")
    void alCancelarSeRetieneSoloSobreLaEstanciaYLosExtrasSeAnulan() {
        Reserva reserva = conExtras(confirmada("RES-2026-00001", 10, 12));
        Folio folio = folioConExtras(reserva);
        pagar(folio, 75_000);

        // 29 h antes: el 50 % de los 200.000 de la estancia, no de los 250.000
        Dinero retenido = new CancelacionDomainService().procesarCancelacion(reserva, folio, politicaCongelada(),
                parametros(3), dia(9).atTime(10, 0));

        assertEquals(Dinero.de(100_000), retenido);
        assertEquals(new Saldo(Dinero.de(25_000), SituacionSaldo.PENDIENTE), folio.saldo());
    }

    @Test
    @Tag("RN-13")
    @Tag("DEC-89")
    void siLaRetencionEsSobreLoPagadoNoAlcanzaLoPagadoPorLosExtras() {
        Reserva reserva = conExtras(confirmada("RES-2026-00001", 10, 12));
        Folio folio = folioConExtras(reserva);
        pagar(folio, 75_000);
        PoliticaCancelacion sobrePagado = new PoliticaCancelacion(new PoliticaId("POL-1"), new AlojamientoId("ALO-1"), 1,
                List.of(new TramoCancelacion(0, porcentaje(BaseRetencion.PAGADO, 100))),
                porcentaje(BaseRetencion.PAGADO, 100));

        Dinero retenido = new CancelacionDomainService().procesarCancelacion(reserva, folio, sobrePagado,
                parametros(3), dia(9).atTime(10, 0));

        // De los 75.000, 60.000 son de la estancia; los 15.000 de los extras quedan a favor del huésped
        assertEquals(Dinero.de(60_000), retenido);
        assertEquals(new Saldo(Dinero.de(15_000), SituacionSaldo.A_FAVOR), folio.saldo());
    }

    @Test
    @Tag("RN-13")
    @Tag("DEC-89")
    void alNoLlegarTambienSeAnulanLosExtras() {
        Reserva reserva = conExtras(confirmada("RES-2026-00001", 10, 12));
        Folio folio = folioConExtras(reserva);
        pagar(folio, 250_000);

        Dinero retenido = new NoShowDomainService().declararNoShow(reserva, folio, politicaCongelada(), parametros(3),
                dia(10).atTime(23, 0), new UsuarioId("USR-1"));

        assertEquals(Dinero.de(200_000), retenido); // el 100 % de la estancia
        assertEquals(new Saldo(Dinero.de(50_000), SituacionSaldo.A_FAVOR), folio.saldo());
    }

    @Test
    @Tag("RN-21")
    @Tag("DEC-89")
    void alVencerSeAnulanEstanciaYExtras() {
        Reserva reserva = conExtras(reserva("RES-2026-00001", APT, 10, 12, EstadoReserva.PENDIENTE));
        Folio folio = folioConExtras(reserva);

        new CancelacionDomainService().procesarVencimiento(reserva, folio, parametros(3), dia(5).atStartOfDay());

        assertTrue(folio.saldo().alDia());
    }
}
