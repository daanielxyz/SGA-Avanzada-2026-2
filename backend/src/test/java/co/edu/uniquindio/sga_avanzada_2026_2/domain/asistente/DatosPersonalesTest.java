package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatosPersonalesTest {

    @ParameterizedTest
    @Tag("DEC-127")
    @CsvSource(delimiter = '|', value = {
            "Escríbele a ana.gomez@correo.com.co mañana | Escríbele a [correo] mañana",
            "Su celular es 3001234567 | Su celular es [teléfono]",
            "Llamó del 300 123 4567 | Llamó del [teléfono]",
            "Su número: +57 300-123-4567. | Su número: [teléfono].",
            "La cédula 1094123456 no sale | La cédula [documento] no sale",
            "Con puntos 1.094.123.456 tampoco | Con puntos [documento] tampoco",
            "Pasaporte AB1234567 de Chile | Pasaporte [documento] de Chile"
    })
    void deberiaOcultarCorreosCelularesYDocumentos(String texto, String esperado) {
        assertEquals(esperado, DatosPersonales.enmascarar(texto));
    }

    @ParameterizedTest
    @Tag("DEC-127")
    @ValueSource(strings = {
            "¿Cómo cancelo la RES-2026-00042 del APT-101?",
            "El titular TIT-12 entra el 2026-12-20 a las 15:00",
            "Registré un pago de $ 500.000 y eran 50.000",
            "Debe 1.500.000 pesos de 3 noches",
            "Pagó $1.200.000 por la estancia"
    })
    void deberiaConservarCodigosFechasYDinero(String texto) {
        assertEquals(texto, DatosPersonales.enmascarar(texto));
    }
}
