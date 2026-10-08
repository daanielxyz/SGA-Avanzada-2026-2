package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoDuplicadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrearApartamentoTest {

    private RepositoriosEnMemoria.Apartamentos apartamentos;
    private CrearApartamento crearApartamento;

    @BeforeEach
    void preparar() {
        apartamentos = new RepositoriosEnMemoria.Apartamentos();
        RepositoriosEnMemoria.Alojamientos alojamientos = new RepositoriosEnMemoria.Alojamientos();
        alojamientos.guardar(RepositoriosEnMemoria.puertaAlSol("ALO-1"));
        crearApartamento = new CrearApartamento(apartamentos, alojamientos);
    }

    private static CrearApartamentoCommand comando(String codigo, String alojamientoId,
                                                   List<CrearApartamentoCommand.ImagenCommand> imagenes) {
        return new CrearApartamentoCommand(codigo, alojamientoId, "Apartamento 101", "Vista al mar", 4, 2, imagenes,
                List.of("Balcón", "Cocina equipada"));
    }

    private static List<CrearApartamentoCommand.ImagenCommand> unaPrincipal() {
        return List.of(new CrearApartamentoCommand.ImagenCommand("https://cdn.sga.co/apt-101.jpg", true));
    }

    @Test
    @Tag("APA-11")
    void deberiaCrearElApartamentoInactivoYDevolverSusDatos() {
        ApartamentoResult resultado = crearApartamento.ejecutar(comando("APT-101", "ALO-1", unaPrincipal()));

        assertEquals("APT-101", resultado.codigo());
        assertEquals("PENDIENTE_PREPARACION", resultado.estadoOperativo());
        assertFalse(resultado.activo());
        assertEquals(List.of("Balcón", "Cocina equipada"), resultado.caracteristicas());
        assertTrue(apartamentos.buscarPorCodigo(new ApartamentoId("APT-101")).isPresent());
    }

    @Test
    void deberiaRechazarCodigoRepetido() {
        crearApartamento.ejecutar(comando("APT-101", "ALO-1", unaPrincipal()));

        assertThrows(RecursoDuplicadoException.class,
                () -> crearApartamento.ejecutar(comando("APT-101", "ALO-1", unaPrincipal())));
    }

    @Test
    @Tag("ALO-07")
    void deberiaRechazarSiElAlojamientoNoExiste() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> crearApartamento.ejecutar(comando("APT-101", "ALO-9", unaPrincipal())));
        assertTrue(apartamentos.guardados.isEmpty());
    }

    @Test
    @Tag("IMG-02")
    void noDeberiaGuardarNadaSiElDominioRechazaLosDatos() {
        List<CrearApartamentoCommand.ImagenCommand> dosPrincipales = List.of(
                new CrearApartamentoCommand.ImagenCommand("https://cdn.sga.co/a.jpg", true),
                new CrearApartamentoCommand.ImagenCommand("https://cdn.sga.co/b.jpg", true));

        assertThrows(ReglaDominioException.class,
                () -> crearApartamento.ejecutar(comando("APT-101", "ALO-1", dosPrincipales)));
        assertTrue(apartamentos.guardados.isEmpty());
    }
}
