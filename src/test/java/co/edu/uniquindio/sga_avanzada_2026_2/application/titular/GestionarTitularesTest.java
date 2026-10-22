package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoDuplicadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Casos de uso del Titular con repositorios en memoria; el alojamiento ALO-1 existe.
 */
class GestionarTitularesTest {

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
        assertEquals(titular, new ConsultarTitularUseCase(titulares).ejecutar("TIT-1"));
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
        ActualizarTitularUseCase actualizar = new ActualizarTitularUseCase(titulares);

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
                () -> new ConsultarTitularUseCase(titulares).ejecutar("TIT-9"));
    }
}
