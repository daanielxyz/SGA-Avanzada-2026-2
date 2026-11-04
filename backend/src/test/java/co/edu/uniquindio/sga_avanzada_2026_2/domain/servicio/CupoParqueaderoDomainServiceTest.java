package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("RP-02")
class CupoParqueaderoDomainServiceTest {

    private static final LocalDate ENTRADA = LocalDate.of(2026, 12, 1);
    private final CupoParqueaderoDomainService servicio = new CupoParqueaderoDomainService();

    private static Reserva conVehiculos(String codigo, LocalDate entrada, LocalDate salida, int vehiculos) {
        Estancia estancia = new Estancia(entrada, salida);
        List<LineaCotizacion> desglose = estancia.noches().stream()
                .map(n -> new LineaCotizacion(n, new TemporadaId("TEM-1"), Dinero.de(100_000), 1, Dinero.de(100_000)))
                .toList();
        return new Reserva(new ReservaId(codigo), new ApartamentoId("APT-101"), new TitularId("TIT-1"), estancia,
                EstadoReserva.CONFIRMADA, CanalOrigen.DIRECTO, null, null,
                List.of(new Ocupante(new OcupanteId("OCU-1"), "Ana", LocalDate.of(1990, 1, 1), null, null)), List.of(),
                null, null, Dinero.de(100_000L * desglose.size()), desglose, new PoliticaId("POL-1"),
                LocalDate.of(2026, 10, 1).atStartOfDay(), vehiculos, 0);
    }

    @Test
    void deberiaAceptarVehiculosHastaLlenarElCupo() {
        Reserva otra = conVehiculos("RES-2026-00001", ENTRADA, ENTRADA.plusDays(2), 2);

        assertDoesNotThrow(() -> servicio.exigirCupo(new Estancia(ENTRADA, ENTRADA.plusDays(1)), 1, List.of(otra), null, 3));
    }

    @Test
    void deberiaRechazarSiEnAlgunaNocheSuperaElCupo() {
        Reserva otra = conVehiculos("RES-2026-00001", ENTRADA.plusDays(1), ENTRADA.plusDays(2), 3);

        assertThrows(ReglaDominioException.class, () -> servicio.exigirCupo(new Estancia(ENTRADA, ENTRADA.plusDays(2)),
                1, List.of(otra), null, 3));
    }

    @Test
    void laNocheDeSalidaNoOcupaParqueadero() {
        Reserva otra = conVehiculos("RES-2026-00001", ENTRADA.minusDays(2), ENTRADA, 3);

        assertDoesNotThrow(() -> servicio.exigirCupo(new Estancia(ENTRADA, ENTRADA.plusDays(1)), 1, List.of(otra), null, 3));
    }

    @Test
    void alModificarNoSeCuentaLaMismaReserva() {
        Reserva misma = conVehiculos("RES-2026-00001", ENTRADA, ENTRADA.plusDays(2), 3);

        assertDoesNotThrow(() -> servicio.exigirCupo(misma.estancia(), 3, List.of(misma), misma, 3));
    }

    @Test
    void sinVehiculosNoHayNadaQueVerificar() {
        assertDoesNotThrow(() -> servicio.exigirCupo(new Estancia(ENTRADA, ENTRADA.plusDays(1)), 0, List.of(), null, 0));
    }
}
