package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.APT;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CambioCapacidadDomainServiceTest {

    private final CambioCapacidadDomainService servicio = new CambioCapacidadDomainService();

    private static Apartamento apartamento(ApartamentoId codigo, int capacidad, boolean activo) {
        return new Apartamento(codigo, new AlojamientoId("ALO-1"), "Apto", null, new Capacidad(capacidad),
                new Dormitorio(2), EstadoOperativo.PREPARADO, List.of(new Imagen("https://cdn.sga.co/apt.jpg", true)),
                List.of(new Caracteristica("Balcón")), List.of(), activo);
    }

    @Test
    @Tag("APA-15")
    @Tag("CAP-04")
    void deberiaCambiarLaCapacidadYDevolverLasReservasQueYaNoCaben() {
        Apartamento apartamento = apartamento(APT, 4, false);
        Reserva deCuatro = reserva("RES-2026-00001", APT, 10, 12, EstadoReserva.CONFIRMADA, 4);
        Reserva deDos = reserva("RES-2026-00002", APT, 14, 16, EstadoReserva.PENDIENTE, 2);

        List<Reserva> afectadas = servicio.cambiarCapacidad(apartamento, new Capacidad(2), List.of(),
                List.of(deCuatro, deDos), 2);

        assertEquals(new Capacidad(2), apartamento.capacidad());
        assertEquals(List.of(deCuatro), afectadas);
        assertEquals(4, deCuatro.ocupantes().size()); // la reserva no cambia (APA-15)
    }

    @Test
    @Tag("APA-15")
    void lasReservasTerminadasNoSeAdvierten() {
        Reserva finalizada = reserva("RES-2026-00001", APT, 10, 12, EstadoReserva.CANCELADA, 4);

        assertEquals(List.of(), servicio.cambiarCapacidad(apartamento(APT, 4, false), new Capacidad(2), List.of(),
                List.of(finalizada), 2));
    }

    @Test
    @Tag("CAP-05")
    void unApartamentoActivoNoPuedeIgualarLaUnicaOtraCapacidad() {
        Apartamento activo = apartamento(APT, 4, true);
        List<Apartamento> activos = List.of(activo, apartamento(new ApartamentoId("APT-102"), 2, true));

        assertThrows(ReglaDominioException.class,
                () -> servicio.cambiarCapacidad(activo, new Capacidad(2), activos, List.of(), 2));
        assertEquals(new Capacidad(4), activo.capacidad());
    }

    @Test
    @Tag("CAP-05")
    void unApartamentoInactivoNoCuentaParaLaVariedad() {
        Apartamento inactivo = apartamento(APT, 4, false);
        List<Apartamento> activos = List.of(apartamento(new ApartamentoId("APT-102"), 2, true),
                apartamento(new ApartamentoId("APT-103"), 3, true));

        servicio.cambiarCapacidad(inactivo, new Capacidad(2), activos, List.of(), 2);

        assertEquals(new Capacidad(2), inactivo.capacidad());
    }
}
