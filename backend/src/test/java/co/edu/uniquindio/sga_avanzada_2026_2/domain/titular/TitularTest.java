package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MotivoRechazo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Rechazos.exigeMotivo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    // --- Nombres y apellidos aparte, e identidad validada (DEC-98) ---

    private static final UsuarioId RECEPCION = new UsuarioId("USR-2");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 9, 15, 0);

    private static Titular anaGomez() {
        return new Titular(ID, ALO, " Ana María ", "Gómez", " ", CC, CORREO, "3001234567", null);
    }

    @Test
    @Tag("DEC-98")
    void losApellidosVanAparteYElNombreLosJunta() {
        Titular titular = anaGomez();

        assertEquals("Ana María", titular.nombres());
        assertEquals("Gómez", titular.primerApellido());
        assertNull(titular.segundoApellido());
        assertEquals("Ana María Gómez", titular.nombre());
        assertEquals("Ana Ríos", new Titular(ID, ALO, "Ana Ríos", CC, CORREO, null).nombre());
    }

    @Test
    @Tag("DEC-98")
    void recepcionValidaLaIdentidadUnaSolaVez() {
        Titular titular = anaGomez();
        assertFalse(titular.estaValidado());

        titular.validarIdentidad(RECEPCION, AHORA);

        assertTrue(titular.estaValidado());
        assertEquals(new ValidacionIdentidad(RECEPCION, AHORA), titular.validacion());
        exigeMotivo(MotivoRechazo.IDENTIDAD_VALIDADA, () -> titular.validarIdentidad(RECEPCION, AHORA.plusHours(1)));
        assertEquals(AHORA, titular.validacion().fechaHora());
    }

    @Test
    @Tag("DEC-68")
    @Tag("DEC-98")
    void sinValidarElHuespedCambiaSuNombreDesdeLaCuenta() {
        Titular titular = anaGomez();

        titular.actualizarDesdeCuenta("Ana", "Gómez", "Ríos", "3119998888");

        assertEquals("Ana Gómez Ríos", titular.nombre());
        assertEquals("3119998888", titular.telefono());
        assertEquals(CORREO, titular.correo());
    }

    @Test
    @Tag("DEC-109")
    void unaCuentaDeAntesSeparaSusApellidosAunqueEsteValidada() {
        Titular deAntes = new Titular(ID, ALO, "Ana María Gómez", CC, CORREO, "3001234567");
        Titular validada = new Titular(ID, ALO, "Ana María Gómez", CC, CORREO, "3001234567");
        validada.validarIdentidad(RECEPCION, AHORA);

        deAntes.actualizarDesdeCuenta("Ana María", "Gómez", null, "3001234567");
        validada.actualizarDesdeCuenta("ana maría", "GÓMEZ", null, "3001234567");

        assertEquals("Ana María", deAntes.nombres());
        assertEquals("Gómez", deAntes.primerApellido());
        assertEquals("ana maría GÓMEZ", validada.nombre(), "es el mismo nombre, solo cambia cómo está escrito");
        assertEquals("GÓMEZ", validada.primerApellido());
    }

    @Test
    @Tag("DEC-98")
    void validadoElHuespedSoloCambiaElTelefonoYElHotelSiCambiaElNombre() {
        Titular titular = anaGomez();
        titular.validarIdentidad(RECEPCION, AHORA);

        titular.actualizarDesdeCuenta("Ana María Gómez", null, null, "3119998888");
        assertEquals("3119998888", titular.telefono());
        assertEquals("Gómez", titular.primerApellido(), "el mismo nombre en un solo texto no borra los apellidos");

        exigeMotivo(MotivoRechazo.IDENTIDAD_VALIDADA,
                () -> titular.actualizarDesdeCuenta("Ana", "Pérez", null, "3119998888"));
        assertEquals("Ana María Gómez", titular.nombre());

        titular.actualizarDatos("Ana María", "Gómez", "Ríos", CORREO, "3119998888");
        assertEquals("Ana María Gómez Ríos", titular.nombre());
        assertTrue(titular.estaValidado());
    }

    @Test
    @Tag("DEC-98")
    void validadoElCorreoSigueCambiandoConPin() {
        Titular titular = anaGomez();
        titular.validarIdentidad(RECEPCION, AHORA);

        titular.cambiarCorreo(new Correo("ana.nueva@correo.co"));

        assertEquals(new Correo("ana.nueva@correo.co"), titular.correo());
    }
}
