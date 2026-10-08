package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Dos transacciones cambian la misma reserva a la vez: la que confirma de última no sobrescribe a la otra, sino que
 * falla por bloqueo optimista (DEC-17). Usa una base H2 propia porque las transacciones se confirman de verdad.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrencia;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"})
class ReservaConcurrenciaTest {

    private static final ReservaId CODIGO = new ReservaId("RES-2026-90001");

    @Autowired
    private ReservaRepository reservas;

    @Autowired
    private PlatformTransactionManager transacciones;

    private static Reserva reserva() {
        Estancia estancia = new Estancia(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));
        List<LineaCotizacion> desglose = estancia.noches().stream()
                .map(n -> new LineaCotizacion(n, new TemporadaId("TEM-1"), Dinero.de(100_000), 1, Dinero.de(100_000)))
                .toList();
        return new Reserva(CODIGO, new ApartamentoId("APT-101"), new TitularId("TIT-1"), estancia,
                EstadoReserva.PENDIENTE, CanalOrigen.DIRECTO, null, null,
                List.of(new Ocupante(new OcupanteId("OCU-1"), "Ana", LocalDate.of(1990, 1, 1), null, null)),
                List.of(), null, null, Dinero.de(200_000), desglose, new PoliticaId("POL-1"),
                LocalDate.of(2026, 10, 1).atStartOfDay());
    }

    @Test
    @Tag("DEC-17")
    void laSegundaTransaccionQueGuardaLaMismaReservaFallaSinSobrescribir() {
        TransactionTemplate primera = new TransactionTemplate(transacciones);
        TransactionTemplate segunda = new TransactionTemplate(transacciones);
        segunda.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        primera.executeWithoutResult(estado -> reservas.guardar(reserva()));

        assertThrows(OptimisticLockingFailureException.class, () -> primera.executeWithoutResult(estado -> {
            Reserva vistaPorRecepcion = reservas.buscarPorCodigo(CODIGO).orElseThrow();
            segunda.executeWithoutResult(otro -> {
                Reserva vistaPorElHuesped = reservas.buscarPorCodigo(CODIGO).orElseThrow();
                vistaPorElHuesped.indicarHoraEstimadaLlegada(LocalTime.of(10, 0));
                reservas.guardar(vistaPorElHuesped);
            });
            vistaPorRecepcion.indicarHoraEstimadaLlegada(LocalTime.of(18, 0));
            reservas.guardar(vistaPorRecepcion);
        }));

        assertEquals(LocalTime.of(10, 0), reservas.buscarPorCodigo(CODIGO).orElseThrow().horaEstimadaLlegada());
    }
}
