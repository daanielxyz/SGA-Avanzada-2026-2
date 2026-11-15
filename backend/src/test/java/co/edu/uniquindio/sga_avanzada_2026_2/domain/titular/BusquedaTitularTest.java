package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cómo recepción encuentra a un titular (DEC-131): por palabras del nombre o por el documento, sin importar
 * mayúsculas, tildes ni separadores del número.
 */
class BusquedaTitularTest {

    @Test
    @Tag("DEC-131")
    void deberiaPartirElTextoEnPalabrasSinTildesNiMayusculas() {
        assertEquals(List.of("ana", "rios"), BusquedaTitular.de("  Ana   RÍOS ana ").palabras());
        assertTrue(BusquedaTitular.de("   ").todos());
        assertTrue(BusquedaTitular.de(null).todos());
    }

    @Test
    @Tag("DEC-131")
    void deberiaEncontrarPorCadaPalabraDelNombreEnCualquierOrden() {
        assertTrue(BusquedaTitular.de("rios ana").encuentra("Ana María Ríos", "1094"));
        assertTrue(BusquedaTitular.de("MARIA").encuentra("Ana María Ríos", "1094"));
        assertFalse(BusquedaTitular.de("ana gomez").encuentra("Ana María Ríos", "1094"));
    }

    @Test
    @Tag("DEC-131")
    void deberiaEncontrarPorElDocumentoAunqueSeEscribaConPuntos() {
        assertTrue(BusquedaTitular.de("1.094.000").encuentra("Ana Ríos", "1094000"));
        assertTrue(BusquedaTitular.de("940").encuentra("Ana Ríos", "1094000"));
        assertTrue(BusquedaTitular.de("ab-123").encuentra("John Smith", "AB123456"));
        assertFalse(BusquedaTitular.de("555").encuentra("Ana Ríos", "1094000"));
    }

    @Test
    @Tag("DEC-131")
    void unTextoMuyLargoSeRecortaSinFallar() {
        BusquedaTitular busqueda = BusquedaTitular.de("a".repeat(150));

        assertEquals(BusquedaTitular.MAXIMO, busqueda.palabras().getFirst().length());
    }
}
