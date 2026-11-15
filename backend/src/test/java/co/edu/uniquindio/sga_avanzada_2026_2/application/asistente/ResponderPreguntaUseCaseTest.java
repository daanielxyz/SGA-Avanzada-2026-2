package co.edu.uniquindio.sga_avanzada_2026_2.application.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Asistente;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.AsistenteNoDisponibleException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Consulta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.MotivoSinIa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Respuesta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.RolConsultante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.SeccionManual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Responder una duda: con la IA, o con el manual si la IA no puede (DEC-127). */
class ResponderPreguntaUseCaseTest {

    private static final SeccionManual FOLIO = new SeccionManual("folio", "El folio", "/reservas/{codigo}/folio",
            Set.of(RolConsultante.RECEPCIONISTA, RolConsultante.ADMINISTRADOR),
            "Para revertir un pago, pulsa «Revertir» en ese abono.");

    private final RepositoriosEnMemoria.Alojamientos alojamientos = new RepositoriosEnMemoria.Alojamientos();
    private final RepositoriosEnMemoria.Politicas politicas = new RepositoriosEnMemoria.Politicas();
    private final AtomicReference<Consulta> recibida = new AtomicReference<>();

    @BeforeEach
    void preparar() {
        alojamientos.guardar(RepositoriosEnMemoria.puertaAlSol("ALO-1"));
    }

    private ResponderPreguntaUseCase con(Asistente asistente) {
        return new ResponderPreguntaUseCase(alojamientos, politicas, () -> List.of(FOLIO), asistente);
    }

    private static ResponderPreguntaCommand pregunta(String rol, String texto) {
        return new ResponderPreguntaCommand("ALO-1", "USR-5", rol, texto, "/reservas/RES-2026-00042/folio",
                List.of(new ResponderPreguntaCommand.Mensaje("PERSONA", "Hola")));
    }

    @Test
    @Tag("DEC-127")
    void deberiaResponderConLaIaYArmarLaConsultaDelRol() {
        ResponderPreguntaResult result = con(consulta -> {
            recibida.set(consulta);
            return Respuesta.deIa("Pulsa «Revertir».", List.of("folio"));
        }).ejecutar(pregunta("RECEPCIONISTA", "¿Cómo revierto un pago?"));

        assertEquals("IA", result.origen());
        assertNull(result.aviso());
        assertEquals(List.of(new ResponderPreguntaResult.Fuente("folio", "El folio", "/reservas/{codigo}/folio")),
                result.fuentes());
        assertEquals(List.of(FOLIO), recibida.get().secciones());
        assertEquals(1, recibida.get().historial().size());
        assertTrue(recibida.get().contexto().contains("Puerta al Sol"));
    }

    @Test
    @Tag("DEC-127")
    void deberiaResponderConElManualSiLaIaNoPuede() {
        ResponderPreguntaResult result = con(consulta -> {
            throw new AsistenteNoDisponibleException(MotivoSinIa.CUOTA_AGOTADA, "429");
        }).ejecutar(pregunta("RECEPCIONISTA", "¿Cómo revierto un pago?"));

        assertEquals("LOCAL", result.origen());
        assertEquals("CUOTA_AGOTADA", result.motivo());
        assertEquals(MotivoSinIa.CUOTA_AGOTADA.aviso(), result.aviso());
        assertEquals("folio", result.fuentes().getFirst().id());
        assertTrue(result.respuesta().contains("Revertir"));
    }

    @Test
    @Tag("DEC-127")
    void noDeberiaPasarleAServicioLasSeccionesDeRecepcion() {
        con(consulta -> {
            recibida.set(consulta);
            return Respuesta.deIa("Eso lo hace recepción.", List.of());
        }).ejecutar(pregunta("PERSONAL_SERVICIO", "¿Cómo revierto un pago?"));

        assertEquals(List.of(), recibida.get().secciones());
    }

    @Test
    @Tag("DEC-127")
    void deberiaRechazarUnAlojamientoQueNoExiste() {
        ResponderPreguntaCommand otro = new ResponderPreguntaCommand("ALO-9", "USR-5", "ADMINISTRADOR", "Hola", "",
                List.of());

        assertThrows(RecursoNoEncontradoException.class,
                () -> con(consulta -> Respuesta.deIa("x", List.of())).ejecutar(otro));
    }
}
