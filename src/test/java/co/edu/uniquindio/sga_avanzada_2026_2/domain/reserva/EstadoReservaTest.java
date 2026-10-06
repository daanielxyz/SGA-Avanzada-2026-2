package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva.CANCELADA;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva.CONFIRMADA;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva.EN_CURSO;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva.FINALIZADA;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva.NO_SHOW;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva.PENDIENTE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadoReservaTest {

    private static final Map<EstadoReserva, Set<EstadoReserva>> PERMITIDAS = Map.of(
            PENDIENTE, EnumSet.of(CONFIRMADA, CANCELADA),
            CONFIRMADA, EnumSet.of(EN_CURSO, CANCELADA, NO_SHOW),
            EN_CURSO, EnumSet.of(FINALIZADA),
            FINALIZADA, EnumSet.noneOf(EstadoReserva.class),
            CANCELADA, EnumSet.noneOf(EstadoReserva.class),
            NO_SHOW, EnumSet.noneOf(EstadoReserva.class));

    @Test
    @Tag("RN-08")
    @Tag("EDO-02")
    void soloDeberiaPermitirLasTransicionesDelCicloDeVida() {
        for (EstadoReserva origen : EstadoReserva.values()) {
            for (EstadoReserva destino : EstadoReserva.values()) {
                assertEquals(PERMITIDAS.get(origen).contains(destino), origen.puedePasarA(destino),
                        origen + " → " + destino);
            }
        }
    }

    @Test
    @Tag("EDO-03")
    void soloLosEstadosActivosRetienenDisponibilidad() {
        Set<EstadoReserva> activos = EnumSet.of(PENDIENTE, CONFIRMADA, EN_CURSO);
        for (EstadoReserva estado : EstadoReserva.values()) {
            assertEquals(activos.contains(estado), estado.retieneDisponibilidad(), estado.name());
        }
    }
}
