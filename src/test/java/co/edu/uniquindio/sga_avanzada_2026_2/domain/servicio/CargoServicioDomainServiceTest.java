package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Ubicacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Cargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.CargoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.TipoCargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.RegistroId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainServiceTest.folioDe;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reserva del 10 al 12 de diciembre por 200.000. El catálogo tiene Desayuno (25.000), Wifi (no genera cargo) y
 * Lavandería (inactivo).
 */
class CargoServicioDomainServiceTest {

    private static final ServicioAdicionalId DESAYUNO = new ServicioAdicionalId("SRV-1");
    private static final ServicioAdicionalId WIFI = new ServicioAdicionalId("SRV-2");
    private static final ServicioAdicionalId LAVANDERIA = new ServicioAdicionalId("SRV-3");

    private final CargoServicioDomainService servicio = new CargoServicioDomainService();

    private static Alojamiento alojamiento() {
        return new Alojamiento(new AlojamientoId("ALO-1"), "Puerta al Sol", "Apartamentos frente al mar",
                "Santa Marta", "Calle 1 # 2-3", new Ubicacion(11.24, -74.21), null, ReservasDePrueba.parametros(4),
                List.of(new ServicioAdicional(DESAYUNO, "Desayuno", true, Dinero.de(25_000), true),
                        new ServicioAdicional(WIFI, "Wifi", false, Dinero.CERO, true),
                        new ServicioAdicional(LAVANDERIA, "Lavandería", true, Dinero.de(18_000), false)),
                List.of(new MedioPago("EFECTIVO"), new MedioPago("TARJETA")));
    }

    private static Reserva enCurso() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        reserva.registrarLlegada(new RegistroId("REG-1"), dia(10).atTime(15, 30), new UsuarioId("USR-1"));
        return reserva;
    }

    @Test
    @Tag("CAR-05")
    @Tag("SERV-02")
    void deberiaCobrarElServicioConSuValorDelMomento() {
        Reserva reserva = enCurso();
        Folio folio = folioDe(reserva);
        Alojamiento alojamiento = alojamiento();

        CargoId cargoId = servicio.cargar(reserva, folio, alojamiento, DESAYUNO, dia(11));
        alojamiento.cambiarValorServicio(DESAYUNO, Dinero.de(30_000));

        Cargo cargo = folio.cargos().getLast();
        assertEquals(cargoId, cargo.id());
        assertEquals(TipoCargo.SERVICIO_ADICIONAL, cargo.tipo());
        assertEquals("Desayuno", cargo.concepto());
        assertEquals(Dinero.de(25_000), cargo.valor());
        assertEquals(Dinero.de(225_000), folio.saldo().monto());
    }

    @Test
    @Tag("DEC-60")
    void noDeberiaCobrarServiciosAUnaReservaQueNoEstaEnCurso() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Folio folio = folioDe(reserva);

        assertThrows(ReglaDominioException.class,
                () -> servicio.cargar(reserva, folio, alojamiento(), DESAYUNO, dia(9)));
        assertEquals(1, folio.cargos().size());
    }

    @Test
    @Tag("SERV-04")
    void noDeberiaCobrarUnServicioInactivo() {
        Reserva reserva = enCurso();

        assertThrows(ReglaDominioException.class,
                () -> servicio.cargar(reserva, folioDe(reserva), alojamiento(), LAVANDERIA, dia(11)));
    }

    @Test
    @Tag("CAR-05")
    void noDeberiaCobrarUnServicioQueNoGeneraCargoNiUnoInexistente() {
        Reserva reserva = enCurso();
        Folio folio = folioDe(reserva);

        assertThrows(ReglaDominioException.class,
                () -> servicio.cargar(reserva, folio, alojamiento(), WIFI, dia(11)));
        assertThrows(ReglaDominioException.class,
                () -> servicio.cargar(reserva, folio, alojamiento(), new ServicioAdicionalId("SRV-9"), dia(11)));
    }

    @Test
    @Tag("FOL-02")
    void noDeberiaCobrarEnElFolioDeOtraReserva() {
        Reserva reserva = enCurso();
        Folio ajeno = folioDe(confirmada("RES-2026-00002", 10, 12));

        assertThrows(ReglaDominioException.class,
                () -> servicio.cargar(reserva, ajeno, alojamiento(), DESAYUNO, dia(11)));
    }
}
