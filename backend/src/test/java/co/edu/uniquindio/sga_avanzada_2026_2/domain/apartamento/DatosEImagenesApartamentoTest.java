package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatosEImagenesApartamentoTest {

    private static final String A = "https://cdn.sga.co/a.jpg";
    private static final String B = "https://cdn.sga.co/b.jpg";

    private static Apartamento apartamento(List<Imagen> imagenes) {
        return Apartamento.crear(new ApartamentoId("APT-101"), new AlojamientoId("ALO-1"), "Suite", null,
                new Capacidad(2), new Dormitorio(1), imagenes, List.of(new Caracteristica("Wifi")));
    }

    /** Uno guardado antes de DEC-123, sin imágenes: se reconstruye, porque crearlo así ya no se puede. */
    private static Apartamento sinImagenes() {
        return new Apartamento(new ApartamentoId("APT-101"), new AlojamientoId("ALO-1"), "Suite", null, new Capacidad(2),
                new Dormitorio(1), EstadoOperativo.PENDIENTE_PREPARACION, List.of(), List.of(new Caracteristica("Wifi")),
                List.of(), false);
    }

    private static List<String> principales(Apartamento a) {
        return a.imagenes().stream().filter(Imagen::principal).map(Imagen::url).toList();
    }

    @Test
    @Tag("CARAC-01")
    void deberiaActualizarLosDatos() {
        Apartamento a = apartamento(List.of(new Imagen(A, true)));

        a.actualizarDatos(" Suite nueva ", "Con vista", new Dormitorio(2), List.of(new Caracteristica("Balcón")));

        assertEquals("Suite nueva", a.nombre());
        assertEquals(2, a.dormitorios().cantidad());
        assertEquals("Balcón", a.caracteristicas().getFirst().nombre());
    }

    @Test
    @Tag("CARAC-01")
    void noDeberiaQuedarSinNombreNiCaracteristicas() {
        Apartamento a = apartamento(List.of(new Imagen(A, true)));

        assertThrows(ReglaDominioException.class,
                () -> a.actualizarDatos(" ", null, new Dormitorio(1), List.of(new Caracteristica("Wifi"))));
        assertThrows(ReglaDominioException.class, () -> a.actualizarDatos("Suite", null, new Dormitorio(1), List.of()));
    }

    @Test
    @Tag("IMG-02")
    void laPrimeraImagenQuedaComoPrincipal() {
        Apartamento a = sinImagenes();

        a.agregarImagen(A);
        a.agregarImagen(B);

        assertEquals(List.of(A), principales(a));
    }

    @Test
    @Tag("IMG-02")
    void noDeberiaRepetirUnaImagen() {
        Apartamento a = apartamento(List.of(new Imagen(A, true)));

        assertThrows(ReglaDominioException.class, () -> a.agregarImagen(A));
    }

    @Test
    @Tag("IMG-02")
    void noDeberiaPasarDeDiezImagenes() {
        Apartamento a = sinImagenes();
        for (int i = 0; i < 10; i++) {
            a.agregarImagen("https://cdn.sga.co/" + i + ".jpg");
        }

        assertThrows(ReglaDominioException.class, () -> a.agregarImagen(A));
    }

    @Test
    @Tag("IMG-02")
    void alMarcarUnaPrincipalLaOtraDejaDeSerlo() {
        Apartamento a = apartamento(List.of(new Imagen(A, true), new Imagen(B, false)));

        a.marcarImagenPrincipal(B);

        assertEquals(List.of(B), principales(a));
        assertThrows(ReglaDominioException.class, () -> a.marcarImagenPrincipal("https://cdn.sga.co/otra.jpg"));
    }

    @Test
    @Tag("IMG-02")
    void siSeQuitaLaPrincipalOtraTomaSuLugar() {
        Apartamento a = apartamento(List.of(new Imagen(A, true), new Imagen(B, false)));

        a.quitarImagen(A);

        assertEquals(List.of(B), principales(a));
    }

    @Test
    @Tag("IMG-01")
    void unApartamentoALaVentaNoSeQuedaSinImagenes() {
        Apartamento a = apartamento(List.of(new Imagen(A, true)));
        a.activar(true);

        assertThrows(ReglaDominioException.class, () -> a.quitarImagen(A));
        assertTrue(a.activo());
    }

    @Test
    @Tag("IMG-01")
    void unApartamentoNuevoNaceConAlMenosUnaImagen() {
        // DEC-123
        assertThrows(ReglaDominioException.class, () -> apartamento(List.of()));
        assertEquals(List.of(A), principales(apartamento(List.of(new Imagen(A, true)))));
    }

    @Test
    @Tag("IMG-01")
    void unApartamentoRetiradoTampocoSeQuedaSinImagenes() {
        // DEC-123: antes solo lo impedía si estaba a la venta
        Apartamento a = apartamento(List.of(new Imagen(A, true), new Imagen(B, false)));

        a.quitarImagen(B);

        assertThrows(ReglaDominioException.class, () -> a.quitarImagen(A));
        assertFalse(a.activo());
        assertEquals(List.of(A), principales(a));
    }
}
