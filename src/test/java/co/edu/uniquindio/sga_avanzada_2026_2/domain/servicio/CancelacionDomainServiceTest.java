package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.SituacionSaldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.parametros;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reserva del 10 al 12 de diciembre por 200.000; la hora de entrada es 15:00 y el anticipo exigido es el 30 %.
 */
class CancelacionDomainServiceTest {

    static final MedioPago EFECTIVO = new MedioPago("EFECTIVO");

    private final CancelacionDomainService servicio = new CancelacionDomainService();

    static Penalizacion porcentaje(BaseRetencion base, int valor) {
        return Penalizacion.porcentaje(base, new Porcentaje(valor));
    }

    /** POL-1: menos de 24 h retiene el 100 %, de 24 a 72 h el 50 %, desde 72 h es gratis. */
    static PoliticaCancelacion politicaCongelada(Penalizacion noShow) {
        return new PoliticaCancelacion(new PoliticaId("POL-1"), new AlojamientoId("ALO-1"), 1, List.of(
                new TramoCancelacion(0, porcentaje(BaseRetencion.VALOR_TOTAL, 100)),
                new TramoCancelacion(24, porcentaje(BaseRetencion.VALOR_TOTAL, 50)),
                new TramoCancelacion(72, porcentaje(BaseRetencion.VALOR_TOTAL, 0))), noShow);
    }

    static PoliticaCancelacion politicaCongelada() {
        return politicaCongelada(porcentaje(BaseRetencion.VALOR_TOTAL, 100));
    }

    static Folio folioDe(Reserva reserva) {
        return Folio.abrir(new FolioId("FOL-1"), reserva.codigo(), reserva.valorTotal(), dia(1));
    }

    static void pagar(Folio folio, long monto) {
        folio.registrarPago(EFECTIVO, Dinero.de(monto), dia(1), dia(1), Set.of(EFECTIVO));
    }

    private Dinero cancelar(Reserva reserva, Folio folio, PoliticaCancelacion politica, LocalDateTime ahora) {
        return servicio.procesarCancelacion(reserva, folio, politica, parametros(3), ahora);
    }

    @Test
    @Tag("RN-13")
    @Tag("RN-12")
    void deberiaAplicarLaRetencionDeLaPoliticaCongeladaAlCancelarReserva() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);

        Dinero retenido = cancelar(reserva, folio, politicaCongelada(), dia(9).atTime(10, 0)); // 29 h antes

        assertEquals(Dinero.de(100_000), retenido);
        assertEquals(EstadoReserva.CANCELADA, reserva.estado());
        assertFalse(reserva.estaActiva());
        assertEquals(new Saldo(Dinero.de(100_000), SituacionSaldo.PENDIENTE), folio.saldo());
    }

    @Test
    @Tag("RN-13")
    void deberiaIgnorarUnaPoliticaNuevaPosteriorALaCreacionDeLaReserva() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);
        PoliticaCancelacion vigente = politicaCongelada().nuevaVersion(new PoliticaId("POL-2"),
                List.of(new TramoCancelacion(0, porcentaje(BaseRetencion.VALOR_TOTAL, 0))),
                porcentaje(BaseRetencion.VALOR_TOTAL, 0), 1);

        assertThrows(ReglaDominioException.class, () -> cancelar(reserva, folio, vigente, dia(9).atTime(10, 0)));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
        assertEquals(1, folio.cargos().size());
    }

    @Test
    @Tag("RN-13")
    void conAntelacionSuficienteNoDeberiaRetenerNada() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);
        pagar(folio, 60_000);

        Dinero retenido = cancelar(reserva, folio, politicaCongelada(), dia(7).atTime(15, 0)); // 72 h antes

        assertEquals(Dinero.CERO, retenido);
        assertEquals(new Saldo(Dinero.de(60_000), SituacionSaldo.A_FAVOR), folio.saldo());
    }

    @Test
    @Tag("DEC-41")
    void deberiaUsarLaBaseYElMontoFijoQueConfiguroElAlojamiento() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);
        pagar(folio, 50_000);
        PoliticaCancelacion depositoNoReembolsable = new PoliticaCancelacion(new PoliticaId("POL-1"),
                new AlojamientoId("ALO-1"), 1, List.of(new TramoCancelacion(0,
                Penalizacion.montoFijo(BaseRetencion.PAGADO, Dinero.de(80_000)))),
                porcentaje(BaseRetencion.PAGADO, 100));

        Dinero retenido = cancelar(reserva, folio, depositoNoReembolsable, dia(2).atTime(9, 0));

        assertEquals(Dinero.de(50_000), retenido); // el fijo de 80.000 no supera lo pagado
        assertEquals(new Saldo(Dinero.CERO, SituacionSaldo.AL_DIA), folio.saldo());
    }

    @Test
    @Tag("FOL-02")
    void deberiaRechazarUnFolioDeOtraReserva() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio ajeno = Folio.abrir(new FolioId("FOL-9"), new ReservaId("RES-2026-00009"), Dinero.de(1), dia(1));

        assertThrows(ReglaDominioException.class,
                () -> cancelar(reserva, ajeno, politicaCongelada(), dia(9).atTime(10, 0)));
    }

    @Test
    @Tag("RN-21")
    void deberiaVencerUnaPendienteYAnularSuAlojamientoSinPenalidad() {
        Reserva reserva = ReservasDePrueba.reserva("RES-2026-00001", ReservasDePrueba.APT, 10, 12,
                EstadoReserva.PENDIENTE);
        Folio folio = folioDe(reserva);
        pagar(folio, 50_000);

        servicio.procesarVencimiento(reserva, folio, parametros(3), dia(2).atTime(0, 1)); // creada el 1 a las 0:00

        assertEquals(EstadoReserva.CANCELADA, reserva.estado());
        assertEquals(new Saldo(Dinero.de(50_000), SituacionSaldo.A_FAVOR), folio.saldo());
    }

    @Test
    @Tag("RN-21")
    void noDeberiaVencerAntesDelPlazoNiTocarElFolio() {
        Reserva reserva = ReservasDePrueba.reserva("RES-2026-00001", ReservasDePrueba.APT, 10, 12,
                EstadoReserva.PENDIENTE);
        Folio folio = folioDe(reserva);

        assertThrows(ReglaDominioException.class,
                () -> servicio.procesarVencimiento(reserva, folio, parametros(3), dia(2).atStartOfDay()));
        assertEquals(EstadoReserva.PENDIENTE, reserva.estado());
        assertEquals(1, folio.cargos().size());
    }
}
