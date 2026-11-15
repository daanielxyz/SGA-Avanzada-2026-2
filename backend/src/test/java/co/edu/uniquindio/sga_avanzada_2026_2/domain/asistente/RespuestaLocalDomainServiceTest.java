package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.ConsultaTest.consulta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La respuesta con el manual, sin IA (DEC-127). */
class RespuestaLocalDomainServiceTest {

    private final RespuestaLocalDomainService local = new RespuestaLocalDomainService();

    @Test
    @Tag("DEC-127")
    void deberiaResponderConLaSeccionQueMasSeParece() {
        Respuesta respuesta = local.responder(
                consulta(RolConsultante.RECEPCIONISTA, "¿Cómo hago una cancelación?", ""), MotivoSinIa.IA_APAGADA);

        assertEquals(OrigenRespuesta.LOCAL, respuesta.origen());
        assertEquals(MotivoSinIa.IA_APAGADA, respuesta.motivo());
        assertEquals(List.of("reservas-cancelar"), respuesta.fuentes());
        assertTrue(respuesta.texto().contains("Pulsa «Cancelar»"));
    }

    @Test
    @Tag("DEC-127")
    void deberiaPreferirLaSeccionDeLaPantallaEnUnEmpate() {
        // «pulsa» está en las dos; la pantalla de la reserva decide
        Respuesta respuesta = local.responder(
                consulta(RolConsultante.ADMINISTRADOR, "¿Qué pulsa uno aquí?", "/reservas/RES-2026-00042"),
                MotivoSinIa.NO_DISPONIBLE);

        assertEquals(List.of("reservas-cancelar"), respuesta.fuentes());
    }

    @Test
    @Tag("DEC-127")
    void deberiaDecirQueNoSabeSiNadaCoincide() {
        Respuesta respuesta = local.responder(
                consulta(RolConsultante.RECEPCIONISTA, "¿Cuál es la clave del wifi?", ""), MotivoSinIa.CUOTA_AGOTADA);

        assertEquals(RespuestaLocalDomainService.SIN_COINCIDENCIA, respuesta.texto());
        assertEquals(List.of(), respuesta.fuentes());
    }

    @Test
    @Tag("DEC-127")
    void noDeberiaUsarLaSeccionDeOtroRol() {
        Respuesta respuesta = local.responder(
                consulta(RolConsultante.PERSONAL_SERVICIO, "¿Cómo cancelo una reserva?", ""), MotivoSinIa.IA_APAGADA);

        assertEquals(List.of(), respuesta.fuentes());
    }

    @Test
    @Tag("DEC-127")
    void deberiaIgualarPalabrasConYSinTildes() {
        assertEquals(RespuestaLocalDomainService.raices("Cancelación"), RespuestaLocalDomainService.raices("cancelar"));
    }
}
