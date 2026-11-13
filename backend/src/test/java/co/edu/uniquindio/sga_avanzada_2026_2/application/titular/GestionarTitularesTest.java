package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoDuplicadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Casos de uso del Titular con repositorios en memoria; el alojamiento ALO-1 existe.
 */
class GestionarTitularesTest {

    /** Solo USR-1 está en el directorio del personal; USR-9 ya no existe. */
    private static final DirectorioPersonal PERSONAL =
            id -> "USR-1".equals(id.valor()) ? Optional.of("Recepción Demo") : Optional.empty();
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneId.of("UTC"));

    private RepositoriosEnMemoria.Titulares titulares;
    private RegistrarTitularUseCase registrar;

    @BeforeEach
    void preparar() {
        titulares = new RepositoriosEnMemoria.Titulares();
        RepositoriosEnMemoria.Alojamientos alojamientos = new RepositoriosEnMemoria.Alojamientos();
        alojamientos.guardar(RepositoriosEnMemoria.puertaAlSol("ALO-1"));
        registrar = new RegistrarTitularUseCase(titulares, alojamientos, new RepositoriosEnMemoria.Codigos());
    }

    private static RegistrarTitularCommand ana(String correo, String telefono) {
        return new RegistrarTitularCommand("ALO-1", "Ana Ríos", "CC", "1094", correo, telefono);
    }

    @Test
    @Tag("TIT-03")
    void deberiaRegistrarUnTitularSinReserva() {
        TitularResult titular = registrar.ejecutar(ana("ana@correo.co", " "));

        assertEquals("TIT-1", titular.id());
        assertEquals("CC", titular.tipoDocumento());
        assertEquals("1094", titular.numeroDocumento());
        assertEquals("ana@correo.co", titular.correo());
        assertNull(titular.telefono());
        assertEquals(titular, new ConsultarTitularUseCase(titulares, PERSONAL).ejecutar("TIT-1"));
    }

    @Test
    @Tag("TIT-05")
    @Tag("DEC-63")
    void noDeberiaRegistrarDosTitularesConElMismoDocumento() {
        registrar.ejecutar(ana("ana@correo.co", null));

        assertThrows(RecursoDuplicadoException.class, () -> registrar.ejecutar(ana(null, "300 123 4567")));
        assertEquals(1, titulares.guardados.size());
    }

    @Test
    @Tag("DEC-49")
    void noDeberiaRegistrarUnTitularSinContactoNiEnUnAlojamientoInexistente() {
        assertThrows(ReglaDominioException.class, () -> registrar.ejecutar(ana("", null)));
        assertThrows(RecursoNoEncontradoException.class, () -> registrar.ejecutar(
                new RegistrarTitularCommand("ALO-9", "Ana Ríos", "CC", "1094", "ana@correo.co", null)));
    }

    @Test
    @Tag("TIT-01")
    @Tag("DEC-49")
    void deberiaActualizarNombreYContactoSinCambiarElDocumento() {
        registrar.ejecutar(ana("ana@correo.co", null));
        ActualizarTitularUseCase actualizar = new ActualizarTitularUseCase(titulares, PERSONAL);

        TitularResult titular = actualizar.ejecutar(
                new ActualizarTitularCommand("TIT-1", "Ana María Ríos", null, "+57 300 123 4567"));

        assertEquals("Ana María Ríos", titular.nombre());
        assertNull(titular.correo());
        assertEquals("+57 300 123 4567", titular.telefono());
        assertEquals("1094", titular.numeroDocumento());
        assertThrows(ReglaDominioException.class,
                () -> actualizar.ejecutar(new ActualizarTitularCommand("TIT-1", "Ana", null, null)));
        assertThrows(RecursoNoEncontradoException.class,
                () -> actualizar.ejecutar(new ActualizarTitularCommand("TIT-9", "Ana", "ana@correo.co", null)));
        assertThrows(RecursoNoEncontradoException.class,
                () -> new ConsultarTitularUseCase(titulares, PERSONAL).ejecutar("TIT-9"));
    }

    @Test
    @Tag("DEC-115")
    void elPersonalLeeElNombreDeQuienValidoLaIdentidad() {
        registrar.ejecutar(ana("ana@correo.co", null));
        assertNull(new ConsultarTitularUseCase(titulares, PERSONAL).ejecutar("TIT-1").validadoPorNombre());

        TitularResult validado = new ValidarIdentidadTitularUseCase(titulares, RELOJ, PERSONAL)
                .ejecutar("TIT-1", "USR-1");

        assertEquals("USR-1", validado.validadoPor());
        assertEquals("Recepción Demo", validado.validadoPorNombre());
        assertEquals(LocalDateTime.of(2026, 11, 13, 15, 0), validado.validadoEn());
        assertEquals("Recepción Demo",
                new ConsultarTitularUseCase(titulares, PERSONAL).ejecutar("TIT-1").validadoPorNombre());
        assertEquals("Recepción Demo", new ActualizarTitularUseCase(titulares, PERSONAL)
                .ejecutar(new ActualizarTitularCommand("TIT-1", "Ana Ríos", "ana@correo.co", "3001234567"))
                .validadoPorNombre());
    }

    @Test
    @Tag("DEC-115")
    void sinElUsuarioEnElDirectorioOParaElHuespedNoHayNombre() {
        registrar.ejecutar(ana("ana@correo.co", null));

        TitularResult validado = new ValidarIdentidadTitularUseCase(titulares, RELOJ, PERSONAL)
                .ejecutar("TIT-1", "USR-9");

        assertEquals("USR-9", validado.validadoPor());
        assertNull(validado.validadoPorNombre());
        assertNull(TitularResult.de(titulares.guardados.values().iterator().next()).validadoPorNombre());
    }
}
