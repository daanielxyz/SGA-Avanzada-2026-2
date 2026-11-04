package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Temporada;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ida y vuelta contra H2 con el esquema de Flyway: lo que se guarda es lo que se carga.
 */
@DataJpaTest
@Import(CalendarioTemporadasRepositoryJpa.class)
class CalendarioTemporadasRepositoryJpaTest {

    private static final AlojamientoId ALOJAMIENTO = new AlojamientoId("ALO-1");
    private static final TemporadaId BASE = new TemporadaId("TEM-BASE");
    private static final TemporadaId ALTA = new TemporadaId("TEM-ALTA");

    @Autowired
    private CalendarioTemporadasRepositoryJpa repositorio;

    @Autowired
    private EntityManager entityManager;

    private static CalendarioTemporadas calendario() {
        return new CalendarioTemporadas(ALOJAMIENTO, List.of(
                new Temporada(BASE, "Base", null, null, true, 0, true),
                new Temporada(ALTA, "Alta", LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 31), false, 3, true)));
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deberiaCargarElCalendarioCompletoTalComoSeGuardo() {
        repositorio.guardar(calendario());
        sincronizar();

        CalendarioTemporadas cargado = repositorio.buscarPorAlojamiento(ALOJAMIENTO).orElseThrow();

        assertEquals(2, cargado.temporadas().size());
        Temporada base = cargado.temporadas().getFirst();
        assertTrue(base.esBase());
        assertNull(base.fechaInicio());
        Temporada alta = cargado.temporadas().get(1);
        assertEquals("Alta", alta.nombre());
        assertEquals(LocalDate.of(2026, 12, 15), alta.fechaInicio());
        assertEquals(LocalDate.of(2026, 12, 31), alta.fechaFin());
        assertEquals(3, alta.estanciaMinimaNoches());
        assertTrue(alta.activa());
        assertEquals(ALTA, cargado.temporadaDe(new Noche(LocalDate.of(2026, 12, 20))).id());
    }

    @Test
    void deberiaPersistirLosCambiosHechosPorElDominio() {
        repositorio.guardar(calendario());
        sincronizar();

        CalendarioTemporadas calendario = repositorio.buscarPorAlojamiento(ALOJAMIENTO).orElseThrow();
        calendario.agregarTemporada(new TemporadaId("TEM-MEDIA"), "Media", LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 7, 15), 0);
        calendario.cambiarFechas(ALTA, LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 31));
        repositorio.guardar(calendario);
        sincronizar();
        calendario = repositorio.buscarPorAlojamiento(ALOJAMIENTO).orElseThrow();
        calendario.desactivarTemporada(ALTA);
        repositorio.guardar(calendario);
        sincronizar();

        CalendarioTemporadas cargado = repositorio.buscarPorAlojamiento(ALOJAMIENTO).orElseThrow();
        assertEquals(3, cargado.temporadas().size());
        assertEquals(1, cargado.cantidadTemporadasEspecificas());
        assertFalse(cargado.temporadas().get(1).activa());
        assertEquals(LocalDate.of(2026, 12, 20), cargado.temporadas().get(1).fechaInicio());
        assertEquals(BASE, cargado.temporadaDe(new Noche(LocalDate.of(2026, 12, 25))).id());
    }

    @Test
    void deberiaDevolverVacioSiNoExiste() {
        assertTrue(repositorio.buscarPorAlojamiento(new AlojamientoId("ALO-9")).isEmpty());
    }
}
