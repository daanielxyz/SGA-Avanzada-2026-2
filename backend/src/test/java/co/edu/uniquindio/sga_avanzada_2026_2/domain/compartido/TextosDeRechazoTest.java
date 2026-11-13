package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.SituacionSaldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Lo que se escribe en un mensaje de rechazo se lee en palabras de recepción, no en códigos (#8 · DEC-118).
 */
class TextosDeRechazoTest {

    @Test
    @Tag("DEC-118")
    void lasFechasYLasHorasSeLeenComoLasEscribeUnaPersona() {
        assertEquals("05/12/2026", FormatoTexto.fecha(LocalDate.of(2026, 12, 5)));
        assertEquals("13/11/2026 a las 22:00", FormatoTexto.fechaHora(LocalDateTime.of(2026, 11, 13, 22, 0)));
        assertEquals("01/01/2027 a las 07:05", FormatoTexto.fechaHora(LocalDateTime.of(2027, 1, 1, 7, 5)));
    }

    @Test
    @Tag("DEC-118")
    void ningunEstadoSeNombraConSuCodigo() {
        List<String> textos = new ArrayList<>();
        Arrays.stream(EstadoReserva.values()).map(EstadoReserva::texto).forEach(textos::add);
        Arrays.stream(EstadoOperativo.values()).map(EstadoOperativo::texto).forEach(textos::add);
        Arrays.stream(SituacionSaldo.values()).map(SituacionSaldo::texto).forEach(textos::add);

        for (String texto : textos) {
            assertFalse(texto.contains("_"), texto);
            assertEquals(texto, texto.toLowerCase(), texto);
            assertFalse(texto.isBlank());
        }
        assertEquals("en curso", EstadoReserva.EN_CURSO.texto());
        assertEquals("pendiente de preparación", EstadoOperativo.PENDIENTE_PREPARACION.texto());
        assertEquals("a favor del huésped", SituacionSaldo.A_FAVOR.texto());
    }
}
