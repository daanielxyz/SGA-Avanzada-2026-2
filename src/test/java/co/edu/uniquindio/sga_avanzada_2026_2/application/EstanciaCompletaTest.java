package co.edu.uniquindio.sga_avanzada_2026_2.application;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.AgregarServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.AgregarServicioAdicionalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.AlojamientoResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ActivarApartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CambiarEstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CambiarEstadoOperativoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ConsultarApartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamentoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.CargarServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.CargarServicioAdicionalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.ConsultarFolio;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.FolioResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RegistrarPago;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RegistrarPagoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConfirmarReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConsultarReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CorregirRegistro;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CorregirRegistroCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CrearReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CrearReservaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.EstanciaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.IndicarHoraLlegada;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.IndicarHoraLlegadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupanteCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegada;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarSalida;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarSalidaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.TitularCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.AgregarTemporada;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.AgregarTemporadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.CalendarioResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.ConsultarCalendario;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DefinirTarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DefinirTarifaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una estancia de punta a punta con Spring, las transacciones reales y la base H2: desde el alojamiento que crea el
 * arranque (DEC-53) hasta la salida. Comprueba que los casos de uso que tocan varios agregados (D-02 · DEC-44)
 * guardan y vuelven a leer todo. Usa una base propia porque las transacciones se confirman de verdad, y el reloj
 * real: la reserva entra hoy.
 */
@SpringBootTest
@TestPropertySource(locations = "file:src/main/resources/application.properties", properties = {
        "spring.datasource.url=jdbc:h2:mem:estancia;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration"})
class EstanciaCompletaTest {

    private static final String ALO = "ALO-1";
    private static final String APT = "APT-101";
    private static final String RECEPCION = "USR-recepcion";

    @Autowired
    private Clock reloj;

    @Autowired
    private CrearApartamento crearApartamento;

    @Autowired
    private ActivarApartamento activarApartamento;

    @Autowired
    private CambiarEstadoOperativo cambiarEstadoOperativo;

    @Autowired
    private ConsultarApartamento consultarApartamento;

    @Autowired
    private ConsultarCalendario consultarCalendario;

    @Autowired
    private AgregarTemporada agregarTemporada;

    @Autowired
    private DefinirTarifa definirTarifa;

    @Autowired
    private AgregarServicioAdicional agregarServicio;

    @Autowired
    private CrearReserva crearReserva;

    @Autowired
    private IndicarHoraLlegada indicarHoraLlegada;

    @Autowired
    private ConfirmarReserva confirmarReserva;

    @Autowired
    private ConsultarReserva consultarReserva;

    @Autowired
    private RegistrarLlegada registrarLlegada;

    @Autowired
    private CorregirRegistro corregirRegistro;

    @Autowired
    private RegistrarSalida registrarSalida;

    @Autowired
    private ConsultarFolio consultarFolio;

    @Autowired
    private RegistrarPago registrarPago;

    @Autowired
    private CargarServicioAdicional cargarServicio;


    /** Un apartamento activo y PREPARADO, con tarifa de 100.000 en todas las temporadas, y el Desayuno a 25.000. */
    private String prepararAlojamiento(LocalDate hoy) {
        crearApartamento.ejecutar(new CrearApartamentoCommand(APT, ALO, "Apto 101", null, 4, 2,
                List.of(new CrearApartamentoCommand.ImagenCommand("https://cdn.sga.co/apt-101.jpg", true)),
                List.of("Balcón")));
        agregarTemporada.ejecutar(new AgregarTemporadaCommand(ALO, "Alta", hoy.plusDays(200), hoy.plusDays(210), 0,
                List.of()));
        agregarTemporada.ejecutar(new AgregarTemporadaCommand(ALO, "Media", hoy.plusDays(300), hoy.plusDays(310), 0,
                List.of()));
        consultarCalendario.ejecutar(ALO).temporadas().stream()
                .map(CalendarioResult.TemporadaResult::id)
                .forEach(temporada -> definirTarifa.ejecutar(
                        new DefinirTarifaCommand(APT, temporada, BigDecimal.valueOf(100_000))));
        activarApartamento.ejecutar(APT);
        cambiarEstadoOperativo.ejecutar(new CambiarEstadoOperativoCommand(APT, "EN_PREPARACION"));
        cambiarEstadoOperativo.ejecutar(new CambiarEstadoOperativoCommand(APT, "PREPARADO"));
        AlojamientoResult alojamiento = agregarServicio.ejecutar(
                new AgregarServicioAdicionalCommand(ALO, "Desayuno", true, BigDecimal.valueOf(25_000)));
        return alojamiento.serviciosAdicionales().getLast().id();
    }

    private void pagar(String codigo, long monto, LocalDate hoy) {
        registrarPago.ejecutar(new RegistrarPagoCommand(codigo, "EFECTIVO", BigDecimal.valueOf(monto), hoy));
    }

    @Test
    @Tag("D-02")
    @Tag("DEC-44")
    @Tag("RN-17")
    void unaEstanciaCompletaQuedaGuardadaEnLosTresAgregados() {
        LocalDate hoy = LocalDate.now(reloj);
        String desayuno = prepararAlojamiento(hoy);

        String codigo = crearReserva.ejecutar(new CrearReservaCommand(APT, hoy, hoy.plusDays(2), "DIRECTO",
                new TitularCommand("Ana Ríos", "CC", "1094", "ana@correo.co", null),
                List.of(new OcupanteCommand("Ana Ríos", LocalDate.of(1990, 5, 1), "CC", "1094", "Colombiana")),
                null)).codigo();
        indicarHoraLlegada.ejecutar(new IndicarHoraLlegadaCommand(codigo, LocalTime.of(16, 0)));
        pagar(codigo, 60_000, hoy); // el 30 % de 200.000
        confirmarReserva.ejecutar(codigo);

        registrarLlegada.ejecutar(new RegistrarLlegadaCommand(codigo, RECEPCION));
        assertEquals("EN_CURSO", consultarReserva.ejecutar(codigo).estado());
        assertEquals("OCUPADO", consultarApartamento.ejecutar(APT).estadoOperativo());

        corregirRegistro.ejecutar(new CorregirRegistroCommand(codigo, LocalDateTime.now(reloj).minusSeconds(1),
                "Llegó antes de que se registrara", RECEPCION));
        cargarServicio.ejecutar(new CargarServicioAdicionalCommand(codigo, desayuno));
        assertThrows(ReglaDominioException.class,
                () -> registrarSalida.ejecutar(new RegistrarSalidaCommand(codigo, RECEPCION, null)));
        assertEquals("EN_CURSO", consultarReserva.ejecutar(codigo).estado());

        pagar(codigo, 165_000, hoy); // 200.000 + 25.000 − 60.000
        EstanciaResult salida = registrarSalida.ejecutar(new RegistrarSalidaCommand(codigo, RECEPCION, null));

        FolioResult folio = consultarFolio.ejecutar(codigo);
        assertEquals("FINALIZADA", consultarReserva.ejecutar(codigo).estado());
        assertEquals("PENDIENTE_PREPARACION", consultarApartamento.ejecutar(APT).estadoOperativo());
        assertEquals(List.of("REG-1", "REG-2"),
                salida.registros().stream().map(EstanciaResult.RegistroResult::id).toList());
        assertEquals("SAL-1", salida.salida().id());
        assertTrue(folio.cerrado());
        assertEquals("AL_DIA", folio.situacionSaldo());
        assertEquals(2, folio.cargos().size());
        assertEquals(2, folio.pagos().size());
        assertEquals(0, BigDecimal.valueOf(225_000).compareTo(folio.totalPagado()));
    }
}
