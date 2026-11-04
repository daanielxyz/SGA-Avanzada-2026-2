package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.AutorizacionCierre;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.RegistroId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.SalidaId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainServiceTest.folioDe;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainServiceTest.pagar;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.apartamentoEn;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reserva del 10 al 12 de diciembre por 200.000, registrada el 10 a las 15:30; el apartamento está OCUPADO.
 */
class SalidaOperativaDomainServiceTest {

    private static final UsuarioId RECEPCION = new UsuarioId("USR-1");
    private static final SalidaId SALIDA = new SalidaId("SAL-1");
    private static final LocalDateTime A_LAS_10 = dia(12).atTime(10, 0);
    private static final AutorizacionCierre AUTORIZACION = new AutorizacionCierre(new UsuarioId("ADM-1"),
            "Pagará por transferencia", A_LAS_10);

    private final SalidaOperativaDomainService servicio = new SalidaOperativaDomainService();

    private static Reserva enCurso() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        reserva.registrarLlegada(new RegistroId("REG-1"), dia(10).atTime(15, 30), RECEPCION);
        return reserva;
    }

    private static Folio folioPagado(Reserva reserva) {
        Folio folio = folioDe(reserva);
        pagar(folio, 200_000);
        return folio;
    }

    @Test
    @Tag("SAL-02")
    @Tag("SAL-03")
    void deberiaFinalizarLaReservaCerrarElFolioYLiberarElApartamento() {
        Reserva reserva = enCurso();
        Apartamento apartamento = apartamentoEn(EstadoOperativo.OCUPADO);
        Folio folio = folioPagado(reserva);

        servicio.procesarCheckOut(reserva, apartamento, folio, SALIDA, A_LAS_10, RECEPCION);

        assertEquals(EstadoReserva.FINALIZADA, reserva.estado());
        assertEquals(A_LAS_10, reserva.salida().fechaHora());
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, apartamento.estadoOperativo());
        assertTrue(folio.cerrado());
    }

    @Test
    @Tag("RN-17")
    @Tag("SAL-02")
    void siFolioCerrarYSaldoFinalDiferenteCeroSinAutorizacionRechazar() {
        Reserva reserva = enCurso();
        Apartamento apartamento = apartamentoEn(EstadoOperativo.OCUPADO);
        Folio folio = folioDe(reserva); // debe los 200.000

        assertThrows(ReglaDominioException.class,
                () -> servicio.procesarCheckOut(reserva, apartamento, folio, SALIDA, A_LAS_10, RECEPCION));
        assertEquals(EstadoReserva.EN_CURSO, reserva.estado());
        assertEquals(EstadoOperativo.OCUPADO, apartamento.estadoOperativo());
        assertFalse(folio.cerrado());
    }

    @Test
    @Tag("RN-17")
    @Tag("AUTC-04")
    void conAutorizacionDelAdministradorSaleConSaldoPendiente() {
        Reserva reserva = enCurso();
        Folio folio = folioDe(reserva);

        servicio.procesarCheckOut(reserva, apartamentoEn(EstadoOperativo.OCUPADO), folio, SALIDA, A_LAS_10, RECEPCION,
                AUTORIZACION);

        assertEquals(EstadoReserva.FINALIZADA, reserva.estado());
        assertTrue(folio.cerrado());
        assertEquals(AUTORIZACION, folio.autorizacion());
        assertFalse(folio.saldo().alDia()); // la autorización no cambia el saldo
    }

    @Test
    @Tag("SAL-02")
    void aceptaUnFolioQueYaSeCerroAntes() {
        Reserva reserva = enCurso();
        Folio folio = folioPagado(reserva);
        folio.cerrar();

        servicio.procesarCheckOut(reserva, apartamentoEn(EstadoOperativo.OCUPADO), folio, SALIDA, A_LAS_10, RECEPCION);

        assertEquals(EstadoReserva.FINALIZADA, reserva.estado());
    }

    @Test
    @Tag("SAL-01")
    void siLaReservaNoEstaEnCursoNoCierraElFolio() {
        Reserva confirmada = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioPagado(confirmada);

        assertThrows(ReglaDominioException.class, () -> servicio.procesarCheckOut(confirmada,
                apartamentoEn(EstadoOperativo.OCUPADO), folio, SALIDA, A_LAS_10, RECEPCION));
        assertFalse(folio.cerrado());
    }

    @Test
    @Tag("SAL-04")
    void unaSalidaAnticipadaNoAplicaLaPoliticaDeCancelacion() {
        Reserva reserva = enCurso();
        Folio folio = folioPagado(reserva);

        servicio.procesarCheckOut(reserva, apartamentoEn(EstadoOperativo.OCUPADO), folio, SALIDA,
                dia(11).atTime(9, 0), RECEPCION);

        assertEquals(EstadoReserva.FINALIZADA, reserva.estado());
        assertEquals(1, folio.cargos().size()); // ni anulación ni penalidad
    }

    @Test
    @Tag("FOL-02")
    void deberiaRechazarUnFolioDeOtraReserva() {
        Reserva reserva = enCurso();
        Folio ajeno = Folio.abrir(new FolioId("FOL-9"), new ReservaId("RES-2026-00009"),
                reserva.valorTotal(), dia(1));

        assertThrows(ReglaDominioException.class, () -> servicio.procesarCheckOut(reserva,
                apartamentoEn(EstadoOperativo.OCUPADO), ajeno, SALIDA, A_LAS_10, RECEPCION));
    }
}
