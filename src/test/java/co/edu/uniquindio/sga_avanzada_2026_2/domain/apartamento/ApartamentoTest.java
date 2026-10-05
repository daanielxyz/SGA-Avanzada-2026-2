package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApartamentoTest {

    private static final Imagen PRINCIPAL = new Imagen("https://cdn.sga.co/apt-101.jpg", true);
    private static final Imagen SECUNDARIA = new Imagen("https://cdn.sga.co/apt-101-b.jpg", false);
    private static final List<Caracteristica> CARACTERISTICAS = List.of(new Caracteristica("Balcón"));

    private static Apartamento apartamento(List<Imagen> imagenes, List<Caracteristica> caracteristicas,
                                           EstadoOperativo estado, boolean activo) {
        return new Apartamento(new ApartamentoId("APT-101"), new AlojamientoId("ALO-1"), "Apartamento 101",
                "Vista al mar", new Capacidad(4), new Dormitorio(2), estado, imagenes, caracteristicas,
                List.of(), activo);
    }

    private static Apartamento inactivo() {
        return apartamento(List.of(PRINCIPAL, SECUNDARIA), CARACTERISTICAS, EstadoOperativo.PREPARADO, false);
    }

    private static Apartamento activo() {
        return apartamento(List.of(PRINCIPAL), CARACTERISTICAS, EstadoOperativo.PREPARADO, true);
    }

    private static LocalDate dia(int d) {
        return LocalDate.of(2026, 10, d);
    }

    // --- Creación

    @Test
    @Tag("APA-11")
    @Tag("RN-11")
    void deberiaNacerInactivoSinBloqueosYPendienteDePreparacion() {
        Apartamento nuevo = Apartamento.crear(new ApartamentoId("APT-101"), new AlojamientoId("ALO-1"), "Apto 101",
                null, new Capacidad(4), new Dormitorio(2), List.of(PRINCIPAL), CARACTERISTICAS);

        assertFalse(nuevo.activo());
        assertTrue(nuevo.bloqueos().isEmpty());
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, nuevo.estadoOperativo());
    }

    @Test
    @Tag("CARAC-01")
    void deberiaRechazarCrearConDatosInvalidos() {
        assertThrows(ReglaDominioException.class, () -> Apartamento.crear(new ApartamentoId("APT-101"),
                new AlojamientoId("ALO-1"), "Apto 101", null, new Capacidad(4), new Dormitorio(2),
                List.of(PRINCIPAL), List.of()));
    }

    // --- Invariantes de construcción

    @Test
    @Tag("IMG-01")
    void deberiaAceptarHastaDiezImagenes() {
        List<Imagen> diez = new ArrayList<>(Collections.nCopies(9, SECUNDARIA));
        diez.add(PRINCIPAL);

        assertEquals(10, apartamento(diez, CARACTERISTICAS, EstadoOperativo.PREPARADO, false).imagenes().size());
    }

    @Test
    @Tag("IMG-01")
    void deberiaRechazarMasDeDiezImagenes() {
        List<Imagen> once = new ArrayList<>(Collections.nCopies(10, SECUNDARIA));
        once.add(PRINCIPAL);

        assertThrows(ReglaDominioException.class,
                () -> apartamento(once, CARACTERISTICAS, EstadoOperativo.PREPARADO, false));
    }

    @Test
    @Tag("IMG-02")
    void deberiaExigirExactamenteUnaImagenPrincipal() {
        assertThrows(ReglaDominioException.class,
                () -> apartamento(List.of(SECUNDARIA), CARACTERISTICAS, EstadoOperativo.PREPARADO, false));
        assertThrows(ReglaDominioException.class,
                () -> apartamento(List.of(PRINCIPAL, PRINCIPAL), CARACTERISTICAS, EstadoOperativo.PREPARADO, false));
    }

    @Test
    @Tag("CARAC-01")
    void deberiaExigirAlMenosUnaCaracteristica() {
        assertThrows(ReglaDominioException.class,
                () -> apartamento(List.of(PRINCIPAL), List.of(), EstadoOperativo.PREPARADO, false));
    }

    // --- Activar y retirar de la venta

    @Test
    @Tag("APA-11")
    @Tag("TAR-03")
    void deberiaActivarseConImagenesYTarifasCompletas() {
        Apartamento apartamento = inactivo();

        apartamento.activar(true);

        assertTrue(apartamento.activo());
    }

    @Test
    @Tag("APA-11")
    @Tag("TAR-03")
    void deberiaRechazarActivarSinTarifasCompletas() {
        assertThrows(ReglaDominioException.class, () -> inactivo().activar(false));
    }

    @Test
    @Tag("IMG-01")
    void deberiaRechazarActivarSinImagenes() {
        Apartamento sinImagenes = apartamento(List.of(), CARACTERISTICAS, EstadoOperativo.PREPARADO, false);

        assertThrows(ReglaDominioException.class, () -> sinImagenes.activar(true));
    }

    @Test
    @Tag("APA-16")
    void deberiaRetirarseDeLaVentaSinReservasActivas() {
        Apartamento apartamento = activo();

        apartamento.retirarDeVenta(false);

        assertFalse(apartamento.activo());
    }

    @Test
    @Tag("APA-16")
    void deberiaRechazarRetirarConReservasActivas() {
        Apartamento apartamento = activo();

        assertThrows(ReglaDominioException.class, () -> apartamento.retirarDeVenta(true));
        assertTrue(apartamento.activo());
    }

    // --- Capacidad y estado operativo

    @Test
    @Tag("RN-02")
    @Tag("CAP-04")
    void deberiaReemplazarLaCapacidadYAplicarElNuevoTope() {
        Apartamento apartamento = activo();

        apartamento.cambiarCapacidad(new Capacidad(2));

        assertTrue(apartamento.admite(2));
        assertFalse(apartamento.admite(3));
    }

    @Test
    @Tag("EOPE-02")
    void deberiaCambiarEstadoOperativoPermitido() {
        Apartamento apartamento = activo();

        apartamento.cambiarEstadoOperativo(EstadoOperativo.OCUPADO);

        assertEquals(EstadoOperativo.OCUPADO, apartamento.estadoOperativo());
    }

    @Test
    @Tag("EOPE-02")
    void deberiaRechazarEstadoOperativoNoPermitido() {
        Apartamento apartamento = apartamento(List.of(PRINCIPAL), CARACTERISTICAS, EstadoOperativo.OCUPADO, true);

        assertThrows(ReglaDominioException.class,
                () -> apartamento.cambiarEstadoOperativo(EstadoOperativo.FUERA_DE_SERVICIO));
        assertEquals(EstadoOperativo.OCUPADO, apartamento.estadoOperativo());
    }

    // --- Bloqueos

    @Test
    @Tag("RN-07")
    @Tag("BLO-03")
    void deberiaQuedarBloqueadoEnLasNochesDelBloqueo() {
        Apartamento apartamento = activo();

        apartamento.registrarBloqueo(new BloqueoId("BLO-1"), dia(10), dia(12), "Mantenimiento");

        assertTrue(apartamento.tieneBloqueoEn(new Noche(dia(11))));
        assertFalse(apartamento.tieneBloqueoEn(new Noche(dia(12))));
    }

    @Test
    @Tag("BLO-05")
    void registrarBloqueoNoDeberiaCambiarElEstadoOperativo() {
        Apartamento apartamento = activo();

        apartamento.registrarBloqueo(new BloqueoId("BLO-1"), dia(10), dia(12), "Mantenimiento");

        assertEquals(EstadoOperativo.PREPARADO, apartamento.estadoOperativo());
    }

    @Test
    @Tag("BLO-02")
    void deberiaRechazarBloqueoConIdRepetido() {
        Apartamento apartamento = activo();
        apartamento.registrarBloqueo(new BloqueoId("BLO-1"), dia(10), dia(12), "Mantenimiento");

        assertThrows(ReglaDominioException.class,
                () -> apartamento.registrarBloqueo(new BloqueoId("BLO-1"), dia(20), dia(22), "Pintura"));
    }

    @Test
    @Tag("BLO-06")
    @Tag("APA-12")
    void deberiaLiberarNochesAlLevantarBloqueoYConservarlo() {
        Apartamento apartamento = activo();
        apartamento.registrarBloqueo(new BloqueoId("BLO-1"), dia(10), dia(12), "Mantenimiento");

        apartamento.levantarBloqueo(new BloqueoId("BLO-1"));

        assertFalse(apartamento.tieneBloqueoEn(new Noche(dia(11))));
        assertEquals(1, apartamento.bloqueos().size());
        assertFalse(apartamento.bloqueos().getFirst().vigente());
    }

    @Test
    @Tag("BLO-06")
    void deberiaRechazarLevantarBloqueoInexistente() {
        assertThrows(ReglaDominioException.class, () -> activo().levantarBloqueo(new BloqueoId("BLO-9")));
    }

    @Test
    @Tag("BLO-06")
    void noDeberiaPermitirModificarLosBloqueosDesdeFuera() {
        Apartamento apartamento = activo();

        assertThrows(UnsupportedOperationException.class, () -> apartamento.bloqueos()
                .add(new Bloqueo(new BloqueoId("BLO-2"), dia(1), dia(2), "Externo", true)));
    }
}
