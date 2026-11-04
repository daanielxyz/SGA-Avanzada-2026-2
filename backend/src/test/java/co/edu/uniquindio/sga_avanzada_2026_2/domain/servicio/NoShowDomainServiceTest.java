package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
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
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.parametros;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reserva del 10 al 12 de diciembre por 200.000; hora límite de no-show 22:00, anticipo exigido 30 % (60.000).
 */
class NoShowDomainServiceTest {

    private static final PoliticaCancelacion RETIENE_EL_ANTICIPO =
            politicaCongelada(porcentaje(BaseRetencion.ANTICIPO_EXIGIDO, 100));

    private final NoShowDomainService servicio = new NoShowDomainService();

    @Test
    @Tag("POL-06")
    @Tag("RN-13")
    void deberiaLiquidarLaConsecuenciaDelNoShowDeLaPoliticaCongelada() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);
        pagar(folio, 60_000);

        Dinero retenido = servicio.declararNoShow(reserva, folio, RETIENE_EL_ANTICIPO, parametros(3),
                dia(10).atTime(22, 0));

        assertEquals(Dinero.de(60_000), retenido);
        assertEquals(EstadoReserva.NO_SHOW, reserva.estado());
        assertEquals(new Saldo(Dinero.CERO, SituacionSaldo.AL_DIA), folio.saldo());
    }

    @Test
    @Tag("RES-16")
    void noDeberiaTocarElFolioSiAunNoLlegaLaHoraLimite() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);

        assertThrows(ReglaDominioException.class, () -> servicio.declararNoShow(reserva, folio,
                RETIENE_EL_ANTICIPO, parametros(3), dia(10).atTime(21, 59)));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
        assertEquals(1, folio.cargos().size());
    }
}
