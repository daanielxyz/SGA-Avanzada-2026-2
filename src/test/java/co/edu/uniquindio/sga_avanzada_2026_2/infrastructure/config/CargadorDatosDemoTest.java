package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ApartamentoDisponibleResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponiblesCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponiblesUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CotizarEstanciaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CotizarEstanciaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Con {@code SGA_DATOS_DEMO=true} el arranque deja cuatro apartamentos a la venta (DEC-81), en una base H2 propia y
 * con los valores reales de {@code src/main/resources/application.properties}, como en un computador de desarrollo.
 */
@SpringBootTest
@TestPropertySource(locations = "file:src/main/resources/application.properties", properties = {
        "spring.datasource.url=jdbc:h2:mem:datosdemo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration",
        // Lo que el .env de cada quien pueda traer no cambia la prueba
        "sga.seguridad.jwt.secreto=secreto-solo-para-las-pruebas-del-sga-0123456789",
        "sga.seguridad.administrador.correo=", "sga.seguridad.administrador.clave=", "sga.correo.modo=local",
        "sga.datos-demo=true"})
class CargadorDatosDemoTest {

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");
    private static final List<LocalDate> UN_ADULTO = List.of(LocalDate.of(1990, 5, 1));

    @Autowired
    private CargadorDatosDemo cargador;

    @Autowired
    private ApartamentoRepository apartamentos;

    @Autowired
    private CalendarioTemporadasRepository calendarios;

    @Autowired
    private BuscarDisponiblesUseCase buscarDisponibles;

    @Autowired
    private CotizarEstanciaUseCase cotizarEstancia;

    @Autowired
    private Clock reloj;

    private BigDecimal unaNocheEnSuiteGuadua(LocalDate noche) {
        return cotizarEstancia.ejecutar(new CotizarEstanciaCommand("APT-102", noche, noche.plusDays(1), UN_ADULTO))
                .total();
    }

    @Test
    @Tag("APA-11")
    @Tag("TEM-04")
    void alArrancarDejaCuatroApartamentosALaVentaConDosTemporadasAltas() {
        assertEquals(4, apartamentos.buscarActivos(ALO).size());
        assertEquals(3, calendarios.buscarPorAlojamiento(ALO).orElseThrow().temporadas().size());
    }

    @Test
    @Tag("RN-05")
    void enTemporadaAltaLaNocheCuestaUn30PorCientoMas() {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate enFinDeAnio = CargadorDatosDemo.proxima(hoy, MonthDay.of(12, 15)).plusDays(5);
        LocalDate trasMitadDeAnio = CargadorDatosDemo.proxima(hoy, MonthDay.of(6, 15)).plusDays(31);

        assertEquals(new BigDecimal("130000"), unaNocheEnSuiteGuadua(enFinDeAnio));
        assertEquals(new BigDecimal("100000"), unaNocheEnSuiteGuadua(trasMitadDeAnio));
    }

    @Test
    @Tag("RN-02")
    void unGrupoGrandeSoloCabeEnLaCasa() {
        LocalDate entrada = LocalDate.now(reloj).plusDays(10);

        List<String> libres = buscarDisponibles.ejecutar(new BuscarDisponiblesCommand(ALO.valor(), entrada,
                entrada.plusDays(2), 5)).stream().map(ApartamentoDisponibleResult::codigo).toList();

        assertEquals(List.of("APT-103"), libres);
    }

    @Test
    void arrancarOtraVezNoDuplicaNada() {
        cargador.run(new DefaultApplicationArguments());

        assertEquals(4, apartamentos.buscarActivos(ALO).size());
        assertEquals(3, calendarios.buscarPorAlojamiento(ALO).orElseThrow().temporadas().size());
    }
}
