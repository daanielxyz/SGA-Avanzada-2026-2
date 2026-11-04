package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoOperativoTest {

    @ParameterizedTest(name = "{0} → {1}")
    @Tag("EOPE-02")
    @CsvSource({
            "PREPARADO, OCUPADO",
            "OCUPADO, PENDIENTE_PREPARACION",
            "PENDIENTE_PREPARACION, EN_PREPARACION",
            "EN_PREPARACION, PREPARADO",
            "PREPARADO, FUERA_DE_SERVICIO",
            "PENDIENTE_PREPARACION, FUERA_DE_SERVICIO",
            "EN_PREPARACION, FUERA_DE_SERVICIO",
            "FUERA_DE_SERVICIO, PENDIENTE_PREPARACION"
    })
    void deberiaPermitirTransicionesValidas(EstadoOperativo origen, EstadoOperativo destino) {
        assertTrue(origen.puedePasarA(destino));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @Tag("EOPE-02")
    @CsvSource({
            "OCUPADO, FUERA_DE_SERVICIO",
            "PREPARADO, PENDIENTE_PREPARACION",
            "OCUPADO, PREPARADO",
            "PENDIENTE_PREPARACION, PREPARADO",
            "EN_PREPARACION, OCUPADO",
            "FUERA_DE_SERVICIO, PREPARADO",
            "FUERA_DE_SERVICIO, FUERA_DE_SERVICIO",
            "PREPARADO, PREPARADO"
    })
    void deberiaRechazarTransicionesNoPermitidas(EstadoOperativo origen, EstadoOperativo destino) {
        assertFalse(origen.puedePasarA(destino));
    }

    @Test
    @Tag("RN-11")
    @Tag("EOPE-03")
    void deberiaPermitirRegistroSoloSiEstaPreparado() {
        assertTrue(EstadoOperativo.PREPARADO.permiteRegistro());
    }

    @ParameterizedTest
    @Tag("RN-11")
    @Tag("EOPE-03")
    @EnumSource(value = EstadoOperativo.class, names = "PREPARADO", mode = EnumSource.Mode.EXCLUDE)
    void noDeberiaPermitirRegistroSiNoEstaPreparado(EstadoOperativo estado) {
        assertFalse(estado.permiteRegistro());
    }
}
