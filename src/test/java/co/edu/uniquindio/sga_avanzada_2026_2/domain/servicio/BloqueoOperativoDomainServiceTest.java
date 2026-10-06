package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.BloqueoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.APT;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.apartamentoActivo;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.reserva;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloqueoOperativoDomainServiceTest {

    private final BloqueoOperativoDomainService servicio = new BloqueoOperativoDomainService();

    @Test
    @Tag("BLO-01")
    void deberiaBloquearNochesSinReservasActivas() {
        Apartamento apartamento = apartamentoActivo();

        servicio.registrarBloqueo(apartamento, new BloqueoId("BLO-1"), dia(12), dia(14), "Pintura",
                List.of(confirmada("RES-2026-00001", 10, 12),
                        reserva("RES-2026-00002", APT, 12, 14, EstadoReserva.CANCELADA)));

        assertTrue(apartamento.tieneBloqueoEn(new Noche(dia(13))));
    }

    @Test
    @Tag("BLO-01")
    void deberiaRechazarBloqueoSobreNochesConReservaActiva() {
        Apartamento apartamento = apartamentoActivo();

        assertThrows(ReglaDominioException.class, () -> servicio.registrarBloqueo(apartamento,
                new BloqueoId("BLO-1"), dia(11), dia(13), "Pintura", List.of(confirmada("RES-2026-00001", 10, 12))));
        assertTrue(apartamento.bloqueos().isEmpty());
    }

    @Test
    @Tag("BLO-02")
    void deberiaRechazarRangoInvalidoAntesDeCompararConReservas() {
        assertThrows(ReglaDominioException.class, () -> servicio.registrarBloqueo(apartamentoActivo(),
                new BloqueoId("BLO-1"), dia(12), dia(12), "Pintura", List.of()));
    }
}
