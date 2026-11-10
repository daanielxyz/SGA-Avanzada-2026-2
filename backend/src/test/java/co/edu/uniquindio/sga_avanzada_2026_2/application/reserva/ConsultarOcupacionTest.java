package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.AL_CREAR;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ENTRADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El tablero sobre la reserva de {@link EscenarioEstancia}: 600.000 de Ana Ríos, del 10 al 13 de octubre en el APT-101.
 */
class ConsultarOcupacionTest {

    private EscenarioEstancia escenario;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioEstancia();
    }

    private List<OcupacionResult.ReservaOcupada> reservasEntre(int desde, int hasta) {
        return new ConsultarOcupacionUseCase(escenario.apartamentos, escenario.reservas, escenario.folios,
                escenario.titulares)
                .ejecutar("ALO-1", ENTRADA.plusDays(desde), ENTRADA.plusDays(hasta), "APT-101", 0)
                .contenido().getFirst().reservas();
    }

    private static void assertMonto(long esperado, BigDecimal real) {
        assertEquals(0, BigDecimal.valueOf(esperado).compareTo(real), "esperado " + esperado + " y fue " + real);
    }

    @Test
    @Tag("CU-24")
    @Tag("BL-09")
    void cadaReservaTraeElNombreDeSuTitularYSuCuenta() {
        String codigo = escenario.reservaConfirmada(); // pagó 180.000

        OcupacionResult.ReservaOcupada reserva = reservasEntre(0, 3).getFirst();

        assertEquals(codigo, reserva.codigo());
        assertEquals("Ana Ríos", reserva.titularNombre());
        assertEquals("PENDIENTE", reserva.situacionSaldo());
        assertMonto(420_000, reserva.saldo());
        assertMonto(180_000, reserva.totalPagado());
    }

    @Test
    @Tag("BL-09")
    void sinNingunPagoDebeTodoYConTodoPagadoEstaAlDia() {
        String codigo = escenario.reservaPendiente();

        OcupacionResult.ReservaOcupada sinPagar = reservasEntre(0, 3).getFirst();
        assertEquals("PENDIENTE", sinPagar.situacionSaldo());
        assertMonto(600_000, sinPagar.saldo());
        assertMonto(0, sinPagar.totalPagado());

        escenario.pagar(codigo, 600_000, AL_CREAR);

        OcupacionResult.ReservaOcupada pagada = reservasEntre(0, 3).getFirst();
        assertEquals("AL_DIA", pagada.situacionSaldo());
        assertMonto(0, pagada.saldo());
    }

    @Test
    @Tag("EST-01")
    void unaReservaFueraDelRangoNoSeDibuja() {
        escenario.reservaPendiente();

        assertTrue(reservasEntre(3, 5).isEmpty()); // la noche de salida no se ocupa
    }
}
