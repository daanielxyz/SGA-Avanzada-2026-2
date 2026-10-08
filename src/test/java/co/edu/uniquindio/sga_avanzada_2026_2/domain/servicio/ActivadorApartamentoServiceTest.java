package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Temporada;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivadorApartamentoServiceTest {

    private static final ApartamentoId APT = new ApartamentoId("APT-101");
    private static final TemporadaId BASE = new TemporadaId("TEM-BASE");
    private static final TemporadaId ALTA = new TemporadaId("TEM-ALTA");
    private static final TemporadaId MEDIA = new TemporadaId("TEM-MEDIA");
    private static final int MINIMO_FICHA = 2;

    private final ActivadorApartamentoService servicio = new ActivadorApartamentoService();

    private static final Apartamento APARTAMENTO = new Apartamento(APT, new AlojamientoId("ALO-1"), "Apto 101",
            null, new Capacidad(4), new Dormitorio(2), EstadoOperativo.PREPARADO,
            List.of(new Imagen("https://cdn.sga.co/apt-101.jpg", true)), List.of(new Caracteristica("Balcón")),
            List.of(), false);

    private static CalendarioTemporadas calendarioConAltaYMedia() {
        return new CalendarioTemporadas(new AlojamientoId("ALO-1"), List.of(
                new Temporada(BASE, "Base", null, null, true, 0, true),
                new Temporada(ALTA, "Alta", LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 31), false, 0, true),
                new Temporada(MEDIA, "Media", LocalDate.of(2026, 6, 15), LocalDate.of(2026, 7, 15), false, 0, true)));
    }

    private static Tarifa tarifa(TemporadaId temporada) {
        return new Tarifa(new TarifaId("T-" + temporada.valor()), APT, temporada, Dinero.de(100_000), 1,
                LocalDate.of(2026, 1, 1));
    }

    @Test
    @Tag("TAR-03")
    @Tag("APA-11")
    void deberiaPermitirActivarConTarifaEnTodasLasTemporadas() {
        assertTrue(servicio.puedeActivarse(APARTAMENTO, calendarioConAltaYMedia(),
                List.of(tarifa(BASE), tarifa(ALTA), tarifa(MEDIA)), MINIMO_FICHA));
    }

    @Test
    @Tag("TAR-03")
    @Tag("APA-11")
    void noDeberiaPermitirActivarSiFaltaTarifaEnUnaTemporada() {
        assertFalse(servicio.puedeActivarse(APARTAMENTO, calendarioConAltaYMedia(),
                List.of(tarifa(BASE), tarifa(ALTA)), MINIMO_FICHA));
    }

    @Test
    @Tag("TAR-03")
    void unaTemporadaInactivaNoExigeTarifa() {
        CalendarioTemporadas calendario = calendarioConAltaYMedia();
        calendario.desactivarTemporada(MEDIA);

        assertTrue(servicio.puedeActivarse(APARTAMENTO, calendario, List.of(tarifa(BASE), tarifa(ALTA)), 1));
    }

    @Test
    @Tag("TEM-04")
    void noDeberiaPermitirActivarSiElCalendarioNoTieneElMinimoDeTemporadas() {
        CalendarioTemporadas soloBaseYAlta = new CalendarioTemporadas(new AlojamientoId("ALO-1"), List.of(
                new Temporada(BASE, "Base", null, null, true, 0, true),
                new Temporada(ALTA, "Alta", LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 31), false, 0, true)));

        assertFalse(servicio.puedeActivarse(APARTAMENTO, soloBaseYAlta, List.of(tarifa(BASE), tarifa(ALTA)),
                MINIMO_FICHA));
        assertTrue(servicio.puedeActivarse(APARTAMENTO, soloBaseYAlta, List.of(tarifa(BASE), tarifa(ALTA)), 1));
    }

    private static Apartamento apartamento(String codigo, int capacidad, boolean activo) {
        return new Apartamento(new ApartamentoId(codigo), new AlojamientoId("ALO-1"), "Apto", null,
                new Capacidad(capacidad), new Dormitorio(2), EstadoOperativo.PREPARADO,
                List.of(new Imagen("https://cdn.sga.co/apt.jpg", true)), List.of(new Caracteristica("Balcón")),
                List.of(), activo);
    }

    private static ParametrosAlojamiento parametros(int minimoCapacidadesDistintas) {
        return new ParametrosAlojamiento(12, LocalTime.of(15, 0), LocalTime.of(11, 0), Duration.ofHours(3),
                Duration.ofHours(24), LocalTime.of(22, 0), new Porcentaje(30), 2, 1, MINIMO_FICHA, 2,
                minimoCapacidadesDistintas);
    }

    @Test
    @Tag("APA-11")
    @Tag("CAP-05")
    void deberiaActivarSiLosActivosQuedanConCapacidadesDistintas() {
        Apartamento nuevo = apartamento("APT-101", 4, false);

        servicio.activar(nuevo, calendarioConAltaYMedia(), List.of(tarifa(BASE), tarifa(ALTA), tarifa(MEDIA)),
                List.of(apartamento("APT-102", 2, true)), parametros(2));

        assertTrue(nuevo.activo());
    }

    @Test
    @Tag("CAP-05")
    void noDeberiaActivarSiTodosLosActivosQuedanConLaMismaCapacidad() {
        Apartamento nuevo = apartamento("APT-101", 4, false);

        assertThrows(ReglaDominioException.class, () -> servicio.activar(nuevo, calendarioConAltaYMedia(),
                List.of(tarifa(BASE), tarifa(ALTA), tarifa(MEDIA)), List.of(apartamento("APT-102", 4, true)),
                parametros(2)));
        assertFalse(nuevo.activo());
    }

    @Test
    @Tag("TAR-03")
    void noDeberiaActivarSinTarifasCompletasAunqueCumplaLaVariedad() {
        Apartamento nuevo = apartamento("APT-101", 4, false);

        assertThrows(ReglaDominioException.class, () -> servicio.activar(nuevo, calendarioConAltaYMedia(),
                List.of(tarifa(BASE)), List.of(), parametros(2)));
    }

    @Test
    @Tag("TAR-03")
    void unaTemporadaNuevaConLaTarifaDeCadaActivoSeAcepta() {
        Tarifa nueva = new Tarifa(new TarifaId("TAR-9"), APT, ALTA, Dinero.de(150_000), 1, LocalDate.of(2026, 1, 1));

        assertDoesNotThrow(() -> servicio.exigirTarifasDeTemporada(List.of(apartamento("APT-101", 4, true)), ALTA,
                List.of(nueva)));
    }

    @Test
    @Tag("TAR-03")
    void unaTemporadaNuevaSinLaTarifaDeUnActivoSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> servicio.exigirTarifasDeTemporada(
                List.of(apartamento("APT-101", 4, true), apartamento("APT-102", 2, true)), ALTA,
                List.of(new Tarifa(new TarifaId("TAR-9"), APT, ALTA, Dinero.de(150_000), 1,
                        LocalDate.of(2026, 1, 1)))));
    }

    @Test
    @Tag("TAR-01")
    void unApartamentoNoPuedeTraerDosTarifasEnLaTemporadaNueva() {
        Tarifa una = new Tarifa(new TarifaId("TAR-8"), APT, ALTA, Dinero.de(150_000), 1, LocalDate.of(2026, 1, 1));
        Tarifa otra = new Tarifa(new TarifaId("TAR-9"), APT, ALTA, Dinero.de(160_000), 1, LocalDate.of(2026, 1, 1));

        assertThrows(ReglaDominioException.class, () -> servicio.exigirTarifasDeTemporada(
                List.of(apartamento("APT-101", 4, true)), ALTA, List.of(una, otra)));
    }
}
