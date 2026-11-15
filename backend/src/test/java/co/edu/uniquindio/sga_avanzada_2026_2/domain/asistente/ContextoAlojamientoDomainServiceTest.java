package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** La configuración del hotel que recibe el asistente (DEC-127). */
class ContextoAlojamientoDomainServiceTest {

    private final ContextoAlojamientoDomainService contexto = new ContextoAlojamientoDomainService();

    private static PoliticaCancelacion politica() {
        return PoliticaCancelacion.crear(new PoliticaId("POL-1"), new AlojamientoId("ALO-1"), List.of(
                        new TramoCancelacion(48, Penalizacion.porcentaje(BaseRetencion.VALOR_TOTAL, new Porcentaje(0))),
                        new TramoCancelacion(0, Penalizacion.porcentaje(BaseRetencion.VALOR_TOTAL, new Porcentaje(50)))),
                Penalizacion.montoFijo(BaseRetencion.PAGADO, Dinero.de(100_000)), 2);
    }

    @Test
    @Tag("DEC-127")
    void deberiaContarLosValoresDeEsteHotel() {
        String texto = contexto.describir(RepositoriosEnMemoria.puertaAlSol("ALO-1"), Optional.of(politica()));

        assertTrue(texto.contains("Alojamiento: Puerta al Sol (Santa Marta)."), texto);
        assertTrue(texto.contains("Hora de entrada: 15:00"), texto);
        assertTrue(texto.contains("Hora límite de llegada"), texto);
        assertTrue(texto.contains("Anticipo para confirmar: 30 %"), texto);
        assertTrue(texto.contains("Edad desde la que se cobra a un ocupante: 12 años"), texto);
        assertTrue(texto.contains("Medios de pago de recepción: EFECTIVO, TARJETA."), texto);
        assertTrue(texto.contains("Desayuno ($ 25.000)"), texto);
        assertTrue(texto.contains("con al menos 48 horas de antelación a la entrada, no se retiene nada; "
                + "con al menos 0 horas de antelación a la entrada, se retiene el 50 % del valor total"), texto);
        assertTrue(texto.contains("Si no se presenta: se retienen $ 100.000, sin pasar de lo pagado."), texto);
        assertTrue(texto.contains("cada reserva usa la versión con que se creó"), texto);
    }

    @Test
    @Tag("DEC-127")
    void deberiaDecirloSiTodaviaNoHayPolitica() {
        String texto = contexto.describir(RepositoriosEnMemoria.puertaAlSol("ALO-1"), Optional.empty());

        assertTrue(texto.contains("Todavía no hay política de cancelación publicada."), texto);
    }
}
