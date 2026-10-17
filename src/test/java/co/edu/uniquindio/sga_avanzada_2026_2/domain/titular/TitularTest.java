package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TitularTest {

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");

    private static final TitularId ID = new TitularId("TIT-1");
    private static final Documento CC = new Documento(TipoDocumento.CC, "1094");
    private static final Correo CORREO = new Correo("ana@correo.co");

    @Test
    @Tag("DEC-49")
    void deberiaAceptarCorreoOTelefonoComoContacto() {
        assertDoesNotThrow(() -> new Titular(ID, ALO, "Ana", CC, CORREO, null));
        assertDoesNotThrow(() -> new Titular(ID, ALO, "Ana", CC, null, "+57 300 123 4567"));
    }

    @Test
    @Tag("DEC-49")
    @Tag("TIT-01")
    void deberiaExigirNombreDocumentoYAlMenosUnContacto() {
        assertThrows(ReglaDominioException.class, () -> new Titular(ID, ALO, "Ana", CC, null, " "));
        assertThrows(ReglaDominioException.class, () -> new Titular(ID, ALO, "Ana", null, CORREO, null));
        assertThrows(ReglaDominioException.class, () -> new Titular(ID, ALO, " ", CC, CORREO, null));
    }

    @Test
    @Tag("DEC-49")
    void deberiaRechazarTelefonosInvalidos() {
        assertThrows(ReglaDominioException.class, () -> new Titular(ID, ALO, "Ana", CC, null, "123"));
        assertThrows(ReglaDominioException.class, () -> new Titular(ID, ALO, "Ana", CC, null, "300-ABC-4567"));
        assertThrows(ReglaDominioException.class, () -> new Titular(ID, ALO, "Ana", CC, null, "300+1234567"));
    }

    @Test
    @Tag("TIT-05")
    void actualizarDatosConservaElDocumentoYExigeUnContacto() {
        Titular titular = new Titular(ID, ALO, "Ana", CC, CORREO, null);

        titular.actualizarDatos("Ana María", null, "3001234567");

        assertEquals("Ana María", titular.nombre());
        assertNull(titular.correo());
        assertEquals("3001234567", titular.telefono());
        assertEquals(CC, titular.documento());
        assertThrows(ReglaDominioException.class, () -> titular.actualizarDatos("Ana", null, null));
        assertEquals("3001234567", titular.telefono());
    }

    @Test
    @Tag("DEC-69")
    void unaReservaNuevaSoloCompletaElContactoQueFalta() {
        Titular conTodo = new Titular(ID, ALO, "Ana", CC, CORREO, "3001234567");
        Titular sinCorreo = new Titular(ID, ALO, "Ana", CC, null, "3001234567");

        conTodo.completarContacto(new Correo("otra@correo.co"), "3119999999");
        sinCorreo.completarContacto(CORREO, "3119999999");

        assertEquals(CORREO, conTodo.correo());
        assertEquals("3001234567", conTodo.telefono());
        assertEquals(CORREO, sinCorreo.correo());
        assertEquals("3001234567", sinCorreo.telefono());
        Titular sinTelefono = new Titular(ID, ALO, "Ana", CC, CORREO, null);
        assertThrows(ReglaDominioException.class, () -> sinTelefono.completarContacto(null, "123"));
    }

    @Test
    @Tag("DEC-69")
    void cambiarCorreoConservaElTelefonoYExigeElCorreoNuevo() {
        Titular titular = new Titular(ID, ALO, "Ana", CC, CORREO, "3001234567");

        titular.cambiarCorreo(new Correo("ana.nueva@correo.co"));

        assertEquals(new Correo("ana.nueva@correo.co"), titular.correo());
        assertEquals("3001234567", titular.telefono());
        assertThrows(ReglaDominioException.class, () -> titular.cambiarCorreo(null));
    }
}
