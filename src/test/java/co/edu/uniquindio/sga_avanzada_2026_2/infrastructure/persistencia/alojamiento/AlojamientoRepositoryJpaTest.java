package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Ubicacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ida y vuelta contra H2 con el esquema de Flyway: lo que se guarda es lo que se carga.
 */
@DataJpaTest
@Import(AlojamientoRepositoryJpa.class)
class AlojamientoRepositoryJpaTest {

    private static final AlojamientoId ID = new AlojamientoId("ALO-1");
    private static final ParametrosAlojamiento PARAMETROS = new ParametrosAlojamiento(12, LocalTime.of(15, 0),
            LocalTime.of(11, 0), Duration.ofHours(3), Duration.ofHours(24), LocalTime.of(22, 0), new Porcentaje(30),
            2, 1);

    @Autowired
    private AlojamientoRepositoryJpa repositorio;

    @Autowired
    private EntityManager entityManager;

    private static Alojamiento puertaAlSol() {
        return new Alojamiento(ID, "Puerta al Sol", "Apartamentos frente al mar", "Santa Marta", "Calle 1 # 2-3",
                new Ubicacion(11.24, -74.21), "No mascotas", PARAMETROS,
                List.of(new ServicioAdicional(new ServicioAdicionalId("SRV-1"), "Desayuno", true, Dinero.de(25_000), true),
                        new ServicioAdicional(new ServicioAdicionalId("SRV-2"), "Wifi", false, Dinero.CERO, true)),
                List.of(new MedioPago("EFECTIVO"), new MedioPago("TARJETA"), new MedioPago("PSE")));
    }

    /** Obliga a escribir en la base y a releer desde ella, no desde la caché de JPA. */
    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deberiaCargarElAgregadoCompletoTalComoSeGuardo() {
        repositorio.guardar(puertaAlSol());
        sincronizar();

        Alojamiento cargado = repositorio.buscarPorId(ID).orElseThrow();

        assertEquals("Puerta al Sol", cargado.nombre());
        assertEquals("Apartamentos frente al mar", cargado.descripcion());
        assertEquals("Santa Marta", cargado.ciudad());
        assertEquals("Calle 1 # 2-3", cargado.direccion());
        assertEquals(new Ubicacion(11.24, -74.21), cargado.ubicacion());
        assertEquals("No mascotas", cargado.normas());
        assertEquals(PARAMETROS, cargado.parametros());
        assertEquals(Set.of(new MedioPago("EFECTIVO"), new MedioPago("TARJETA"), new MedioPago("PSE")),
                new HashSet<>(cargado.mediosPago()));
        ServicioAdicional desayuno = cargado.serviciosAdicionales().getFirst();
        assertEquals(new ServicioAdicionalId("SRV-1"), desayuno.id());
        assertEquals("Desayuno", desayuno.nombre());
        assertTrue(desayuno.generaCargo());
        assertEquals(Dinero.de(25_000), desayuno.valor());
        assertTrue(desayuno.activo());
        assertEquals(2, cargado.serviciosAdicionales().size());
    }

    @Test
    void deberiaPersistirLosCambiosHechosPorElDominio() {
        repositorio.guardar(puertaAlSol());
        sincronizar();

        Alojamiento alojamiento = repositorio.buscarPorId(ID).orElseThrow();
        alojamiento.deshabilitarMedioPago(new MedioPago("EFECTIVO"));
        alojamiento.habilitarMedioPago(new MedioPago("NEQUI"));
        alojamiento.cambiarValorServicio(new ServicioAdicionalId("SRV-1"), Dinero.de(30_000));
        alojamiento.desactivarServicio(new ServicioAdicionalId("SRV-2"));
        alojamiento.cambiarUbicacion(new Ubicacion(4.53, -75.68));
        repositorio.guardar(alojamiento);
        sincronizar();

        Alojamiento cargado = repositorio.buscarPorId(ID).orElseThrow();
        assertFalse(cargado.aceptaMedioPago(new MedioPago("EFECTIVO")));
        assertTrue(cargado.aceptaMedioPago(new MedioPago("NEQUI")));
        assertEquals(3, cargado.mediosPago().size());
        assertEquals(Dinero.de(30_000), cargado.serviciosAdicionales().getFirst().valor());
        assertFalse(cargado.serviciosAdicionales().get(1).activo());
        assertEquals(new Ubicacion(4.53, -75.68), cargado.ubicacion());
    }

    @Test
    void deberiaDevolverVacioSiNoExiste() {
        assertTrue(repositorio.buscarPorId(new AlojamientoId("ALO-9")).isEmpty());
    }
}
