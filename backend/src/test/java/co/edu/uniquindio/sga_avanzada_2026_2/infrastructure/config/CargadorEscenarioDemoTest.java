package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.CobroParqueadero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ModoPagoEnLinea;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una base de datos de demo creada antes de V15 quedó sin parqueadero ni mascotas; al arrancar se completa con los
 * valores iniciales, pero lo que ya configuró el administrador no se toca.
 */
class CargadorEscenarioDemoTest {

    private static final SgaPropiedades.Parametros INICIALES = new SgaPropiedades.Parametros(12,
            LocalTime.of(15, 0), LocalTime.of(11, 0), 3, 24, LocalTime.of(22, 0), 30, 2, 1, 2, 2, 2, "ANTICIPO", 30,
            8, true, 2, BigDecimal.valueOf(30_000), "POR_NOCHE", BigDecimal.valueOf(15_000),
            BigDecimal.valueOf(8_000));

    private static ParametrosAlojamiento sinParqueaderoNiMascotas() {
        return new ParametrosAlojamiento(10, LocalTime.of(14, 0), LocalTime.of(12, 0), Duration.ofHours(2),
                Duration.ofHours(48), LocalTime.of(21, 0), new Porcentaje(50), 2, 1, 2, 2, 2,
                ModoPagoEnLinea.TOTAL, Duration.ofMinutes(20));
    }

    @Test
    @Tag("BL-23")
    void unaBaseSinParqueaderoNiMascotasTomaLosValoresInicialesYConservaLoDemas() {
        ParametrosAlojamiento antes = sinParqueaderoNiMascotas();

        ParametrosAlojamiento despues = CargadorEscenarioDemo.conParqueaderoYMascotas(antes, INICIALES);

        assertEquals(8, despues.cupoParqueadero());
        assertTrue(despues.aceptaMascotas());
        assertEquals(2, despues.limiteMascotas());
        assertEquals(Dinero.de(30_000), despues.cargoMascota());
        assertEquals(CobroParqueadero.POR_NOCHE, despues.cobroParqueadero());
        assertEquals(Dinero.de(15_000), despues.cargoCarro());
        assertEquals(Dinero.de(8_000), despues.cargoMoto());
        assertEquals(10, despues.umbralEdadFacturable());
        assertEquals(new Porcentaje(50), despues.anticipo());
        assertEquals(ModoPagoEnLinea.TOTAL, despues.pagoEnLinea());
        assertEquals(Duration.ofMinutes(20), despues.plazoPagoEnLinea());
    }

    @Test
    @Tag("BL-23")
    void siYaTieneParqueaderoOMascotasNoSeToca() {
        ParametrosAlojamiento p = sinParqueaderoNiMascotas();
        ParametrosAlojamiento conCupo = new ParametrosAlojamiento(p.umbralEdadFacturable(), p.horaEntrada(),
                p.horaSalida(), p.tiempoPreparacion(), p.plazoConfirmacion(), p.horaLimiteNoShow(), p.anticipo(),
                p.minimoMediosPago(), p.minimoServiciosAdicionales(), p.minimoTemporadas(),
                p.minimoTramosCancelacion(), p.minimoCapacidadesDistintas(), p.pagoEnLinea(), p.plazoPagoEnLinea(), 3,
                false, 0, Dinero.CERO);

        assertSame(conCupo, CargadorEscenarioDemo.conParqueaderoYMascotas(conCupo, INICIALES));
    }
}
