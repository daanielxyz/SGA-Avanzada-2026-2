package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Casos de uso con los que el administrador configura el alojamiento (CU-30 · CU-50 · CU-53).
 */
class ConfigurarAlojamientoTest {

    private RepositoriosEnMemoria.Alojamientos alojamientos;

    @BeforeEach
    void preparar() {
        alojamientos = new RepositoriosEnMemoria.Alojamientos();
        alojamientos.guardar(RepositoriosEnMemoria.puertaAlSol("ALO-1"));
    }

    @Test
    void deberiaConsultarElAlojamientoYRechazarUnoInexistente() {
        ConsultarAlojamiento consultar = new ConsultarAlojamiento(alojamientos);

        assertEquals("Puerta al Sol", consultar.ejecutar("ALO-1").nombre());
        assertEquals(30, consultar.ejecutar("ALO-1").parametros().anticipoPct());
        assertThrows(RecursoNoEncontradoException.class, () -> consultar.ejecutar("ALO-9"));
    }

    @Test
    @Tag("ALO-03")
    void deberiaReemplazarLosParametros() {
        ParametrosCommand nuevos = new ParametrosCommand(10, LocalTime.of(14, 0), LocalTime.of(12, 0), 2, 48,
                LocalTime.of(21, 0), 0, 2, 1, 2, 2, 1);

        AlojamientoResult resultado = new CambiarParametrosAlojamiento(alojamientos)
                .ejecutar(new CambiarParametrosAlojamientoCommand("ALO-1", nuevos));

        assertEquals(10, resultado.parametros().umbralEdadFacturable());
        assertEquals(48, resultado.parametros().plazoConfirmacionHoras());
        assertEquals(1, resultado.parametros().minimoCapacidadesDistintas());
    }

    @Test
    @Tag("ALO-06")
    void noDeberiaAceptarMinimosQueElCatalogoNoCumple() {
        ParametrosCommand tresMedios = new ParametrosCommand(12, LocalTime.of(15, 0), LocalTime.of(11, 0), 3, 24,
                LocalTime.of(22, 0), 30, 3, 1, 2, 2, 2);

        assertThrows(ReglaDominioException.class, () -> new CambiarParametrosAlojamiento(alojamientos)
                .ejecutar(new CambiarParametrosAlojamientoCommand("ALO-1", tresMedios)));
    }

    @Test
    @Tag("UBI-03")
    void deberiaReemplazarLaUbicacion() {
        AlojamientoResult resultado = new CambiarUbicacionAlojamiento(alojamientos)
                .ejecutar(new CambiarUbicacionAlojamientoCommand("ALO-1", 4.53, -75.67));

        assertEquals(4.53, resultado.latitud());
        assertEquals(-75.67, resultado.longitud());
    }

    @Test
    @Tag("MPAG-06")
    void deberiaHabilitarYDeshabilitarMediosDePago() {
        new HabilitarMedioPago(alojamientos).ejecutar(new MedioPagoCommand("ALO-1", "nequi"));
        AlojamientoResult resultado = new DeshabilitarMedioPago(alojamientos)
                .ejecutar(new MedioPagoCommand("ALO-1", "TARJETA"));

        assertEquals(List.of("EFECTIVO", "NEQUI"), resultado.mediosPago());
    }

    @Test
    @Tag("MPAG-01")
    void noDeberiaDejarElCatalogoPorDebajoDelMinimo() {
        assertThrows(ReglaDominioException.class, () -> new DeshabilitarMedioPago(alojamientos)
                .ejecutar(new MedioPagoCommand("ALO-1", "EFECTIVO")));
    }

    @Test
    @Tag("SERV-01")
    @Tag("SERV-02")
    @Tag("SERV-04")
    void deberiaAgregarCambiarYDesactivarServicios() {
        RepositoriosEnMemoria.Codigos codigos = new RepositoriosEnMemoria.Codigos();
        codigos.siguiente(SerieCodigo.SERVICIO); // SRV-1 ya lo usa el alojamiento de prueba
        AlojamientoResult conSpa = new AgregarServicioAdicional(alojamientos, codigos)
                .ejecutar(new AgregarServicioAdicionalCommand("ALO-1", "Spa", true, BigDecimal.valueOf(80_000)));
        String spa = conSpa.serviciosAdicionales().getLast().id();

        new CambiarValorServicio(alojamientos)
                .ejecutar(new CambiarValorServicioCommand("ALO-1", spa, BigDecimal.valueOf(90_000)));
        AlojamientoResult resultado = new DesactivarServicio(alojamientos)
                .ejecutar(new DesactivarServicioCommand("ALO-1", "SRV-1"));

        AlojamientoResult.ServicioResult cambiado = resultado.serviciosAdicionales().getLast();
        assertEquals(spa, cambiado.id());
        assertEquals(0, BigDecimal.valueOf(90_000).compareTo(cambiado.valor()));
        assertFalse(resultado.serviciosAdicionales().getFirst().activo());
    }

    @Test
    @Tag("SERV-05")
    void noDeberiaDesactivarElUnicoServicioActivo() {
        assertThrows(ReglaDominioException.class, () -> new DesactivarServicio(alojamientos)
                .ejecutar(new DesactivarServicioCommand("ALO-1", "SRV-1")));
    }
}
