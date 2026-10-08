package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El arranque crea el alojamiento con los valores reales de {@code src/main/resources/application.properties}
 * (DEC-53), en una base H2 propia para no dejar datos en la que comparten las demás pruebas.
 */
@SpringBootTest
@TestPropertySource(locations = "file:src/main/resources/application.properties", properties = {
        "spring.datasource.url=jdbc:h2:mem:inicializador;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration"})
class InicializadorAlojamientoTest {

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");

    @Autowired
    private InicializadorAlojamiento inicializador;

    @Autowired
    private AlojamientoRepository alojamientos;

    @Autowired
    private PoliticaCancelacionRepository politicas;

    @Autowired
    private CalendarioTemporadasRepository calendarios;

    @Test
    @Tag("ALO-01")
    @Tag("DEC-41")
    void alArrancarCreaElAlojamientoConLosValoresIniciales() {
        Alojamiento alojamiento = alojamientos.buscarPorId(ALO).orElseThrow();
        PoliticaCancelacion politica = politicas.buscarVigente(ALO).orElseThrow();

        assertEquals("Puerta al Sol", alojamiento.nombre());
        assertEquals(LocalTime.of(15, 0), alojamiento.parametros().horaEntrada());
        assertEquals(2, alojamiento.parametros().minimoCapacidadesDistintas());
        assertEquals(Set.of(new MedioPago("EFECTIVO"), new MedioPago("TRANSFERENCIA")),
                Set.copyOf(alojamiento.mediosPago())); // conjunto sin orden (DEC-27)
        assertEquals(1, politica.version());
        assertEquals(new Porcentaje(0), politica.penalizacionPara(48).porcentaje());
        assertEquals(new Porcentaje(50), politica.penalizacionPara(47).porcentaje());
        assertEquals(BaseRetencion.VALOR_TOTAL, politica.penalizacionNoShow().base());
        assertTrue(calendarios.buscarPorAlojamiento(ALO).orElseThrow().temporadas().getFirst().esBase());
    }

    @Test
    @Tag("ALO-01")
    void arrancarOtraVezNoCreaOtraPolitica() {
        inicializador.run(new DefaultApplicationArguments());

        assertEquals(1, politicas.buscarVigente(ALO).orElseThrow().version());
    }
}
