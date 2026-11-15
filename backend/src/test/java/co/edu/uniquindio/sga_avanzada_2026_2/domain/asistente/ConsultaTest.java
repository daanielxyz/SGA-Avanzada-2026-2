package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La pregunta, el historial, las secciones del manual y la consulta que las junta (DEC-127). */
class ConsultaTest {

    static final SeccionManual CANCELAR = new SeccionManual("reservas-cancelar", "Cancelar una reserva",
            "/reservas/{codigo}", Set.of(RolConsultante.RECEPCIONISTA, RolConsultante.ADMINISTRADOR),
            "Pulsa «Cancelar» en el detalle de la reserva. La ventana dice cuánto se retiene.");
    static final SeccionManual PREPARAR = new SeccionManual("preparacion", "Preparación de apartamentos",
            "/preparacion", Set.of(RolConsultante.PERSONAL_SERVICIO, RolConsultante.ADMINISTRADOR),
            "Pulsa «Empezar a preparar» y luego «Marcar como preparado».");

    static Consulta consulta(RolConsultante rol, String pregunta, String pantalla) {
        return new Consulta(new UsuarioId("USR-1"), rol, new Pregunta(pregunta), pantalla, List.of(),
                List.of(CANCELAR, PREPARAR), "Hora de entrada: 15:00.");
    }

    @Test
    @Tag("DEC-127")
    void deberiaGuardarLaPreguntaSinDatosPersonales() {
        assertEquals("¿Qué hago con la de [correo]?", new Pregunta("  ¿Qué hago con la de ana@correo.com?  ").texto());
    }

    @Test
    @Tag("DEC-127")
    void deberiaRechazarUnaPreguntaVaciaOLarga() {
        assertThrows(ReglaDominioException.class, () -> new Pregunta("   "));
        assertThrows(ReglaDominioException.class, () -> new Pregunta("a".repeat(Pregunta.LARGO_MAXIMO + 1)));
    }

    @Test
    @Tag("DEC-127")
    void deberiaEnmascararTambienElHistorial() {
        assertEquals("Su celular es [teléfono]",
                new MensajeConversacion(AutorMensaje.PERSONA, "Su celular es 3001234567").texto());
        assertThrows(ReglaDominioException.class, () -> new MensajeConversacion(null, "hola"));
    }

    @Test
    @Tag("DEC-127")
    void deberiaDarleACadaRolSoloSusSecciones() {
        Consulta deServicio = consulta(RolConsultante.PERSONAL_SERVICIO, "¿Cómo cancelo?", "/preparacion");
        Consulta deAdministracion = consulta(RolConsultante.ADMINISTRADOR, "¿Cómo cancelo?", "/");

        assertEquals(List.of(PREPARAR), deServicio.secciones());
        assertFalse(deServicio.incluye("reservas-cancelar"));
        assertEquals(List.of(CANCELAR, PREPARAR), deAdministracion.secciones());
        assertTrue(deAdministracion.incluye("reservas-cancelar"));
    }

    @Test
    @Tag("DEC-127")
    void deberiaQuedarseConLosUltimosMensajes() {
        List<MensajeConversacion> veinte = IntStream.rangeClosed(1, 20)
                .mapToObj(n -> new MensajeConversacion(AutorMensaje.PERSONA, "Mensaje " + n))
                .toList();

        Consulta consulta = new Consulta(new UsuarioId("USR-1"), RolConsultante.RECEPCIONISTA, new Pregunta("¿Y ahora?"),
                null, veinte, List.of(CANCELAR), null);

        assertEquals(Consulta.HISTORIAL_MAXIMO, consulta.historial().size());
        assertEquals("Mensaje 11", consulta.historial().getFirst().texto());
        assertEquals("", consulta.pantalla());
    }

    @Test
    @Tag("DEC-127")
    void deberiaRechazarUnaSeccionSinRoles() {
        assertThrows(ReglaDominioException.class,
                () -> new SeccionManual("x", "Título", "/", Set.of(), "Contenido"));
    }

    @Test
    @Tag("DEC-127")
    void deberiaDecirElMotivoSoloEnUnaRespuestaDelManual() {
        assertEquals(OrigenRespuesta.IA, Respuesta.deIa("Hola", List.of()).origen());
        assertThrows(ReglaDominioException.class,
                () -> new Respuesta("Hola", List.of(), OrigenRespuesta.IA, MotivoSinIa.NO_DISPONIBLE));
        assertThrows(ReglaDominioException.class, () -> new Respuesta("Hola", List.of(), OrigenRespuesta.LOCAL, null));
    }
}
