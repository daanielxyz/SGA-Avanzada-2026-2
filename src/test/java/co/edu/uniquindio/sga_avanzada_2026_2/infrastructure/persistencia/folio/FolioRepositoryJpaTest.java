package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.AutorizacionCierre;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Cargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.CargoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Pago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.PagoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.SentidoAjuste;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.TipoCargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.TipoPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ida y vuelta contra H2 con el esquema de Flyway: lo que se guarda es lo que se carga.
 */
@DataJpaTest
@Import(FolioRepositoryJpa.class)
class FolioRepositoryJpaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 12, 1);
    private static final ReservaId RESERVA = new ReservaId("RES-2026-00001");
    private static final MedioPago TARJETA = new MedioPago("TARJETA");

    @Autowired
    private FolioRepositoryJpa repositorio;

    @Autowired
    private FolioJpaRepository filas;

    @Autowired
    private EntityManager entityManager;

    private static Folio folio(String id, ReservaId reserva) {
        return Folio.abrir(new FolioId(id), reserva, Dinero.de(400_000), HOY);
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deberiaCargarElFolioCompletoTalComoSeGuardo() {
        Folio folio = folio("FOL-1", RESERVA);
        CargoId desayuno = folio.agregarServicioAdicional("Desayuno", Dinero.de(25_000), HOY);
        folio.revertirCargo(desayuno, "No lo tomó", HOY);
        PagoId abono = folio.registrarPago(TARJETA, Dinero.de(100_000), HOY, HOY, Set.of(TARJETA));
        folio.revertirPago(abono, HOY, HOY);
        AutorizacionCierre autorizacion = new AutorizacionCierre(new UsuarioId("ADM-1"), "Cortesía",
                HOY.atTime(18, 0));
        folio.cerrar(autorizacion);
        repositorio.guardar(folio);
        sincronizar();

        Folio cargado = repositorio.buscarPorReserva(RESERVA).orElseThrow();

        assertEquals(new FolioId("FOL-1"), cargado.id());
        assertEquals(3, cargado.cargos().size());
        Cargo correccion = cargado.cargos().getLast();
        assertEquals(TipoCargo.AJUSTE, correccion.tipo());
        assertEquals(SentidoAjuste.DISMINUYE, correccion.sentido());
        assertEquals(desayuno, correccion.corrigeA());
        assertNull(cargado.cargos().getFirst().sentido());
        Pago reverso = cargado.pagos().getLast();
        assertEquals(TipoPago.REVERSO, reverso.tipo());
        assertEquals(abono, reverso.reversaDe());
        assertEquals(TARJETA, reverso.medio());
        assertEquals(folio.saldo(), cargado.saldo());
        assertTrue(cargado.cerrado());
        assertEquals(autorizacion, cargado.autorizacion());
    }

    @Test
    void deberiaPersistirLosMovimientosNuevosEIncrementarLaVersion() {
        repositorio.guardar(folio("FOL-1", RESERVA));
        sincronizar();
        long versionInicial = filas.findById("FOL-1").orElseThrow().getVersion();
        sincronizar();

        Folio folio = repositorio.buscarPorReserva(RESERVA).orElseThrow();
        folio.registrarPago(TARJETA, Dinero.de(400_000), HOY, HOY, Set.of(TARJETA));
        folio.cerrar();
        repositorio.guardar(folio);
        sincronizar();

        Folio cargado = repositorio.buscarPorReserva(RESERVA).orElseThrow();
        assertTrue(cargado.saldo().alDia());
        assertTrue(cargado.cerrado());
        assertEquals(versionInicial + 1, filas.findById("FOL-1").orElseThrow().getVersion());
    }

    @Test
    @Tag("FOL-02")
    void unaReservaNoPuedeTenerDosFolios() {
        repositorio.guardar(folio("FOL-1", RESERVA));
        sincronizar();

        repositorio.guardar(folio("FOL-2", RESERVA));

        assertThrows(PersistenceException.class, this::sincronizar);
    }

    @Test
    void deberiaDevolverVacioSiLaReservaNoTieneFolio() {
        assertTrue(repositorio.buscarPorReserva(new ReservaId("RES-2026-99999")).isEmpty());
    }
}
