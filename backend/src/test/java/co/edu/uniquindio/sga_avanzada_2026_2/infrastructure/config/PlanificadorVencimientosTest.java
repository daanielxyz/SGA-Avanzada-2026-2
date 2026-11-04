package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarReservasVencidasUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.VencerReservaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El planificador con los casos de uso reales y repositorios en memoria (CU-20 · DEC-57 · DEC-69): la reserva de
 * recepción de {@link EscenarioEstancia} vence pasadas 24 horas sin confirmarse, y no antes.
 */
class PlanificadorVencimientosTest {

    private static PlanificadorVencimientos planificador(EscenarioEstancia escenario, Clock reloj) {
        return new PlanificadorVencimientos(new BuscarReservasVencidasUseCase(escenario.cargador, escenario.reservas,
                reloj),
                new VencerReservaUseCase(escenario.cargador, escenario.reservas, escenario.folios,
                        new CancelacionDomainService(), reloj),
                "ALO-1");
    }

    @Test
    @Tag("RN-21")
    @Tag("DEC-57")
    void venceLasPendientesQueSuperaronSuPlazoYNadaMas() {
        EscenarioEstancia escenario = new EscenarioEstancia();
        String codigo = escenario.reservaPendiente();

        assertEquals(0, planificador(escenario, EscenarioEstancia.relojEn(
                java.time.LocalDate.of(2026, 10, 2).atTime(9, 0))).vencerPendientes());
        assertEquals(EstadoReserva.PENDIENTE, escenario.cargador.reserva(codigo).estado());

        assertEquals(1, planificador(escenario, EscenarioEstancia.relojEn(
                java.time.LocalDate.of(2026, 10, 2).atTime(10, 1))).vencerPendientes());
        assertEquals(EstadoReserva.CANCELADA, escenario.cargador.reserva(codigo).estado());
    }
}
