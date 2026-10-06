package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalendarioTemporadasTest {

    private static final AlojamientoId ALOJAMIENTO = new AlojamientoId("ALO-1");
    private static final TemporadaId BASE = new TemporadaId("TEM-BASE");
    private static final TemporadaId ALTA = new TemporadaId("TEM-ALTA");
    private static final TemporadaId MEDIA = new TemporadaId("TEM-MEDIA");

    private static LocalDate dia(int mes, int d) {
        return LocalDate.of(2026, mes, d);
    }

    private static Temporada base() {
        return new Temporada(BASE, "Base", null, null, true, 0, true);
    }

    /** Calendario con la base y Alta del 15 al 31 de diciembre. */
    private static CalendarioTemporadas calendario() {
        return new CalendarioTemporadas(ALOJAMIENTO, List.of(base(),
                new Temporada(ALTA, "Alta", dia(12, 15), dia(12, 31), false, 3, true)));
    }

    @Test
    @Tag("TEM-08")
    void deberiaExigirExactamenteUnaTemporadaBase() {
        assertThrows(ReglaDominioException.class, () -> new CalendarioTemporadas(ALOJAMIENTO, List.of()));
        assertThrows(ReglaDominioException.class, () -> new CalendarioTemporadas(ALOJAMIENTO,
                List.of(base(), new Temporada(new TemporadaId("OTRA"), "Base 2", null, null, true, 0, true))));
    }

    @Test
    @Tag("TEM-02")
    void deberiaRechazarCalendarioConTemporadasSolapadas() {
        assertThrows(ReglaDominioException.class, () -> new CalendarioTemporadas(ALOJAMIENTO, List.of(base(),
                new Temporada(ALTA, "Alta", dia(12, 15), dia(12, 31), false, 0, true),
                new Temporada(MEDIA, "Media", dia(12, 1), dia(12, 15), false, 0, true))));
    }

    @Test
    @Tag("TEM-02")
    void deberiaAgregarTemporadaContigua() {
        CalendarioTemporadas calendario = calendario();

        calendario.agregarTemporada(MEDIA, "Media", dia(12, 1), dia(12, 14), 0);

        assertEquals(2, calendario.cantidadTemporadasEspecificas());
    }

    @Test
    @Tag("TEM-02")
    void deberiaRechazarAgregarTemporadaQueSeSolapa() {
        CalendarioTemporadas calendario = calendario();

        assertThrows(ReglaDominioException.class,
                () -> calendario.agregarTemporada(MEDIA, "Media", dia(12, 1), dia(12, 15), 0));
        assertEquals(1, calendario.cantidadTemporadasEspecificas());
    }

    @Test
    @Tag("TEM-02")
    void deberiaRechazarTemporadaConIdRepetido() {
        assertThrows(ReglaDominioException.class,
                () -> calendario().agregarTemporada(ALTA, "Otra", dia(6, 1), dia(6, 30), 0));
    }

    @Test
    @Tag("TEM-03")
    @Tag("TEM-11")
    void deberiaAsignarLaEspecificaOLaBaseATodaNoche() {
        CalendarioTemporadas calendario = calendario();

        assertEquals(ALTA, calendario.temporadaDe(new Noche(dia(12, 20))).id());
        assertEquals(BASE, calendario.temporadaDe(new Noche(dia(12, 14))).id());
        assertEquals(BASE, calendario.temporadaDe(new Noche(dia(3, 1))).id());
    }

    @Test
    @Tag("TEM-05")
    void deberiaCambiarLasFechasDeUnaTemporada() {
        CalendarioTemporadas calendario = calendario();

        calendario.cambiarFechas(ALTA, dia(12, 20), dia(12, 31));

        assertEquals(BASE, calendario.temporadaDe(new Noche(dia(12, 16))).id());
    }

    @Test
    @Tag("TEM-02")
    void deberiaRechazarCambiarFechasSiQuedaSolapada() {
        CalendarioTemporadas calendario = calendario();
        calendario.agregarTemporada(MEDIA, "Media", dia(12, 1), dia(12, 14), 0);

        assertThrows(ReglaDominioException.class, () -> calendario.cambiarFechas(ALTA, dia(12, 10), dia(12, 31)));
        assertEquals(ALTA, calendario.temporadaDe(new Noche(dia(12, 15))).id());
    }

    @Test
    @Tag("TEM-09")
    void deberiaRechazarDarleFechasALaBase() {
        assertThrows(ReglaDominioException.class, () -> calendario().cambiarFechas(BASE, dia(1, 1), dia(1, 5)));
    }

    @Test
    @Tag("TEM-06")
    @Tag("TEM-11")
    void alDesactivarUnaTemporadaSusNochesVuelvenALaBase() {
        CalendarioTemporadas calendario = calendario();

        calendario.desactivarTemporada(ALTA);

        assertEquals(BASE, calendario.temporadaDe(new Noche(dia(12, 20))).id());
        assertEquals(2, calendario.temporadas().size());
        assertEquals(1, calendario.temporadasActivas().size());
    }

    @Test
    @Tag("TEM-10")
    void deberiaRechazarDesactivarLaBase() {
        assertThrows(ReglaDominioException.class, () -> calendario().desactivarTemporada(BASE));
    }

    @Test
    @Tag("RP-01")
    @Tag("TEM-07")
    void deberiaExigirElMinimoSiAlgunaNocheCaeEnTemporadaConMinimo() {
        CalendarioTemporadas calendario = calendario();

        assertEquals(3, calendario.estanciaMinimaPara(new Estancia(dia(12, 14), dia(12, 16))));
        assertEquals(0, calendario.estanciaMinimaPara(new Estancia(dia(12, 10), dia(12, 15))));
    }

    @Test
    @Tag("TEM-06")
    void deberiaRechazarOperarSobreUnaTemporadaInexistente() {
        assertThrows(ReglaDominioException.class,
                () -> calendario().desactivarTemporada(new TemporadaId("NO-EXISTE")));
    }
}
