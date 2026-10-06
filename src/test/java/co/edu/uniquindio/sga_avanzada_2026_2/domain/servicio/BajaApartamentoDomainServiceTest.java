package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.APT;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.apartamentoActivo;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.reserva;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BajaApartamentoDomainServiceTest {

    private final BajaApartamentoDomainService servicio = new BajaApartamentoDomainService();

    @Test
    @Tag("APA-16")
    void deberiaRetirarDeVentaSiSoloTieneReservasTerminadas() {
        Apartamento apartamento = apartamentoActivo();

        servicio.retirarDeVenta(apartamento, List.of(reserva("RES-2026-00001", APT, 1, 3, EstadoReserva.NO_SHOW),
                reserva("RES-2026-00002", APT, 5, 7, EstadoReserva.CANCELADA)));

        assertFalse(apartamento.activo());
    }

    @Test
    @Tag("APA-16")
    void deberiaRechazarRetirarConReservasActivasOFuturas() {
        Apartamento apartamento = apartamentoActivo();

        assertThrows(ReglaDominioException.class,
                () -> servicio.retirarDeVenta(apartamento, List.of(confirmada("RES-2026-00001", 20, 22))));
        assertTrue(apartamento.activo());
    }
}
