package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarApartamentoTest {

    @Test
    void deberiaDevolverElApartamentoExistente() {
        RepositoriosEnMemoria.Apartamentos apartamentos = new RepositoriosEnMemoria.Apartamentos();
        RepositoriosEnMemoria.Alojamientos alojamientos = new RepositoriosEnMemoria.Alojamientos();
        alojamientos.guardar(RepositoriosEnMemoria.puertaAlSol("ALO-1"));
        new CrearApartamento(apartamentos, alojamientos).ejecutar(new CrearApartamentoCommand("APT-101", "ALO-1",
                "Apartamento 101", null, 4, 2,
                List.of(new CrearApartamentoCommand.ImagenCommand("https://cdn.sga.co/apt-101.jpg", true)),
                List.of("Balcón")));

        ApartamentoResult resultado = new ConsultarApartamento(apartamentos).ejecutar("APT-101");

        assertEquals("Apartamento 101", resultado.nombre());
        assertEquals(4, resultado.capacidad());
    }

    @Test
    void deberiaRechazarSiNoExiste() {
        ConsultarApartamento consultar = new ConsultarApartamento(new RepositoriosEnMemoria.Apartamentos());

        assertThrows(RecursoNoEncontradoException.class, () -> consultar.ejecutar("APT-999"));
    }
}
