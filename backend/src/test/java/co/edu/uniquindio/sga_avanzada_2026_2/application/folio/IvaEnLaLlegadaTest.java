package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ImpuestoAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Cargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.TipoCargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La llegada carga el IVA de la estancia si el alojamiento lo cobra (DEC-133); apagado, no carga nada. */
@Tag("DEC-133")
class IvaEnLaLlegadaTest {

    private final EscenarioEstancia escenario = new EscenarioEstancia();

    private void cobrarIva(boolean cobra) {
        Alojamiento alojamiento = escenario.alojamientos.buscarPorId(new AlojamientoId("ALO-1")).orElseThrow();
        alojamiento.cambiarImpuesto(new ImpuestoAlojamiento(cobra, new Porcentaje(19)));
        escenario.alojamientos.guardar(alojamiento);
    }

    private List<Cargo> ivas(String codigo) {
        Reserva reserva = escenario.cargador.reserva(codigo);
        return escenario.cargador.folioDe(reserva).cargos().stream().filter(c -> c.tipo() == TipoCargo.IMPUESTO).toList();
    }

    private String llegar() {
        String codigo = escenario.reservaConfirmada();
        escenario.registrarLlegada(EscenarioEstancia.AL_LLEGAR)
                .ejecutar(new RegistrarLlegadaCommand(codigo, EscenarioEstancia.RECEPCION, "Cuenta por cobrar"));
        return codigo;
    }

    @Test
    void conElIvaEncendidoLaLlegadaLoCargaSobreLasNoches() {
        cobrarIva(true);
        String codigo = llegar();

        Reserva reserva = escenario.cargador.reserva(codigo);
        assertEquals(1, ivas(codigo).size());
        assertEquals(reserva.valorTotal().porcentaje(new Porcentaje(19)), ivas(codigo).getFirst().valor());
        assertEquals("IVA del alojamiento (19 %)", ivas(codigo).getFirst().concepto());
    }

    @Test
    void conElIvaApagadoNoCargaNada() {
        String codigo = llegar();

        assertTrue(ivas(codigo).isEmpty());
    }
}
