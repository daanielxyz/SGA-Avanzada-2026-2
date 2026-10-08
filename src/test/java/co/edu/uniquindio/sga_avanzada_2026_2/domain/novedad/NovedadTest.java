package co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NovedadTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final UsuarioId SERVICIO = new UsuarioId("USR-3");
    private static final UsuarioId ADMIN = new UsuarioId("ADM-1");

    private static Novedad novedad() {
        return Novedad.registrar(new NovedadId("NOV-1"), new ApartamentoId("APT-101"), "Grifo del baño gotea",
                GravedadNovedad.MEDIA, SERVICIO, AHORA);
    }

    @Test
    @Tag("NOV-01")
    void deberiaRegistrarseAbiertaConAutorYFecha() {
        Novedad novedad = novedad();

        assertEquals(EstadoNovedad.ABIERTA, novedad.estado());
        assertEquals(SERVICIO, novedad.autor());
        assertEquals(AHORA, novedad.registradaEn());
        assertThrows(ReglaDominioException.class, () -> Novedad.registrar(new NovedadId("NOV-2"),
                new ApartamentoId("APT-101"), " ", GravedadNovedad.BAJA, SERVICIO, AHORA));
    }

    @Test
    @Tag("NOV-04")
    void deberiaAvanzarElCicloDejandoAutorYFechaEnCadaPaso() {
        Novedad novedad = novedad();

        novedad.avanzar(ADMIN, AHORA.plusHours(1));
        novedad.avanzar(ADMIN, AHORA.plusDays(1));

        assertEquals(EstadoNovedad.CERRADA, novedad.estado());
        assertEquals(List.of(EstadoNovedad.ABIERTA, EstadoNovedad.EN_REVISION, EstadoNovedad.CERRADA),
                novedad.historial().stream().map(CambioEstadoNovedad::estado).toList());
        assertEquals(ADMIN, novedad.historial().getLast().autor());
    }

    @Test
    @Tag("NOV-06")
    void unaNovedadCerradaNoAvanzaNiRetrocedeEnElTiempo() {
        Novedad novedad = novedad();

        assertThrows(ReglaDominioException.class, () -> novedad.avanzar(ADMIN, AHORA.minusMinutes(1)));
        novedad.avanzar(ADMIN, AHORA);
        novedad.avanzar(ADMIN, AHORA);
        assertThrows(ReglaDominioException.class, () -> novedad.avanzar(ADMIN, AHORA.plusDays(2)));
    }

    @Test
    @Tag("NOV-04")
    void noDeberiaReconstruirseConUnHistorialFueraDeOrden() {
        assertThrows(ReglaDominioException.class, () -> new Novedad(new NovedadId("NOV-1"),
                new ApartamentoId("APT-101"), "Grifo", GravedadNovedad.BAJA,
                List.of(new CambioEstadoNovedad(EstadoNovedad.EN_REVISION, SERVICIO, AHORA))));
    }
}
