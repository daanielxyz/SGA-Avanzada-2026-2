package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Bloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.BloqueoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ida y vuelta contra H2 con el esquema de Flyway: lo que se guarda es lo que se carga.
 */
@DataJpaTest
@Import(ApartamentoRepositoryJpa.class)
class ApartamentoRepositoryJpaTest {

    private static final ApartamentoId CODIGO = new ApartamentoId("APT-101");

    @Autowired
    private ApartamentoRepositoryJpa repositorio;

    @Autowired
    private ApartamentoJpaRepository filas;

    @Autowired
    private EntityManager entityManager;

    private static Apartamento apartamento() {
        return new Apartamento(CODIGO, new AlojamientoId("ALO-1"), "Apartamento 101", "Vista al mar",
                new Capacidad(4), new Dormitorio(2), EstadoOperativo.PREPARADO,
                List.of(new Imagen("https://cdn.sga.co/apt-101.jpg", true),
                        new Imagen("https://cdn.sga.co/apt-101-b.jpg", false)),
                List.of(new Caracteristica("Balcón"), new Caracteristica("Cocina equipada")),
                List.of(new Bloqueo(new BloqueoId("BLO-1"), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3),
                        "Pintura", true)),
                true);
    }

    /** Obliga a escribir en la base y a releer desde ella, no desde la caché de JPA. */
    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deberiaCargarElAgregadoCompletoTalComoSeGuardo() {
        repositorio.guardar(apartamento());
        sincronizar();

        Apartamento cargado = repositorio.buscarPorCodigo(CODIGO).orElseThrow();

        assertEquals(CODIGO, cargado.codigo());
        assertEquals(new AlojamientoId("ALO-1"), cargado.alojamientoId());
        assertEquals("Apartamento 101", cargado.nombre());
        assertEquals("Vista al mar", cargado.descripcion());
        assertEquals(new Capacidad(4), cargado.capacidad());
        assertEquals(new Dormitorio(2), cargado.dormitorios());
        assertEquals(EstadoOperativo.PREPARADO, cargado.estadoOperativo());
        assertTrue(cargado.activo());
        assertEquals(apartamento().imagenes(), cargado.imagenes());
        assertEquals(apartamento().caracteristicas(), cargado.caracteristicas());
        Bloqueo bloqueo = cargado.bloqueos().getFirst();
        assertEquals(new BloqueoId("BLO-1"), bloqueo.id());
        assertEquals(LocalDate.of(2026, 11, 1), bloqueo.fechaInicio());
        assertEquals(LocalDate.of(2026, 11, 3), bloqueo.fechaFin());
        assertEquals("Pintura", bloqueo.motivo());
        assertTrue(bloqueo.vigente());
    }

    @Test
    void deberiaPersistirLosCambiosHechosPorElDominio() {
        repositorio.guardar(apartamento());
        sincronizar();

        Apartamento apartamento = repositorio.buscarPorCodigo(CODIGO).orElseThrow();
        apartamento.levantarBloqueo(new BloqueoId("BLO-1"));
        apartamento.registrarBloqueo(new BloqueoId("BLO-2"), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                "Mantenimiento");
        apartamento.cambiarEstadoOperativo(EstadoOperativo.FUERA_DE_SERVICIO);
        apartamento.cambiarCapacidad(new Capacidad(3));
        repositorio.guardar(apartamento);
        sincronizar();

        Apartamento cargado = repositorio.buscarPorCodigo(CODIGO).orElseThrow();
        assertEquals(EstadoOperativo.FUERA_DE_SERVICIO, cargado.estadoOperativo());
        assertEquals(new Capacidad(3), cargado.capacidad());
        assertEquals(2, cargado.bloqueos().size());
        assertFalse(cargado.tieneBloqueoEn(new Noche(LocalDate.of(2026, 11, 1))));
        assertTrue(cargado.tieneBloqueoEn(new Noche(LocalDate.of(2026, 12, 2))));
    }

    @Test
    void deberiaIncrementarLaVersionEnCadaActualizacion() {
        repositorio.guardar(apartamento());
        sincronizar();
        long versionInicial = filas.findById(CODIGO.valor()).orElseThrow().getVersion();
        sincronizar();

        Apartamento apartamento = repositorio.buscarPorCodigo(CODIGO).orElseThrow();
        apartamento.cambiarEstadoOperativo(EstadoOperativo.OCUPADO);
        repositorio.guardar(apartamento);
        sincronizar();

        assertEquals(versionInicial + 1, filas.findById(CODIGO.valor()).orElseThrow().getVersion());
    }

    @Test
    void deberiaDevolverVacioSiNoExiste() {
        assertTrue(repositorio.buscarPorCodigo(new ApartamentoId("APT-999")).isEmpty());
    }

    private static Apartamento otro(String codigo, String alojamiento, boolean activo) {
        return new Apartamento(new ApartamentoId(codigo), new AlojamientoId(alojamiento), "Apto", null,
                new Capacidad(2), new Dormitorio(1), EstadoOperativo.PREPARADO, List.of(),
                List.of(new Caracteristica("Balcón")), List.of(), activo);
    }

    @Test
    @Tag("ALO-07")
    void deberiaListarPorPaginasSoloLosDelAlojamiento() {
        IntStream.rangeClosed(1, 11).forEach(i -> repositorio.guardar(otro("APT-" + (200 + i), "ALO-1", false)));
        repositorio.guardar(otro("APT-900", "ALO-2", false));
        sincronizar();

        Pagina<Apartamento> primera = repositorio.listarPorAlojamiento(new AlojamientoId("ALO-1"), 0);
        Pagina<Apartamento> segunda = repositorio.listarPorAlojamiento(new AlojamientoId("ALO-1"), 1);

        assertEquals(11, primera.totalElementos());
        assertEquals(Pagina.TAMANO, primera.contenido().size());
        assertEquals("APT-201", primera.contenido().getFirst().codigo().valor());
        assertEquals(List.of(new ApartamentoId("APT-211")),
                segunda.contenido().stream().map(Apartamento::codigo).toList());
    }

    @Test
    @Tag("TAR-03")
    void deberiaBuscarSoloLosActivosDelAlojamiento() {
        repositorio.guardar(otro("APT-201", "ALO-1", true));
        repositorio.guardar(otro("APT-202", "ALO-1", false));
        repositorio.guardar(otro("APT-901", "ALO-2", true));
        sincronizar();

        assertEquals(List.of(new ApartamentoId("APT-201")), repositorio.buscarActivos(new AlojamientoId("ALO-1"))
                .stream().map(Apartamento::codigo).toList());
    }
}
