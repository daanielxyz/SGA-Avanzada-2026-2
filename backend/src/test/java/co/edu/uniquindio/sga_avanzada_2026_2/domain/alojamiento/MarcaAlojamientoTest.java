package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("BL-05")
class MarcaAlojamientoTest {

    private static MarcaAlojamiento.Colores conAcento(String acento) {
        return new MarcaAlojamiento.Colores(null, null, null, null, null, null, acento, null, null, null);
    }

    private static MarcaAlojamiento conImagen(String url) {
        return new MarcaAlojamiento(conAcento(null), new MarcaAlojamiento.Fuentes(null, null),
                new MarcaAlojamiento.Radios(null, null, null), url, null);
    }

    @Test
    void sinNadaPropioEsNeutraYConUnSoloValorYaNo() {
        assertTrue(MarcaAlojamiento.neutra().esNeutra());
        assertFalse(new MarcaAlojamiento(conAcento("#F4B942"), new MarcaAlojamiento.Fuentes(null, null),
                new MarcaAlojamiento.Radios(null, null, null), null, null).esNeutra());
    }

    @Test
    void aceptaColoresFuentesRadiosEImagenesValidos() {
        assertDoesNotThrow(() -> conAcento("#f4b942"));
        assertDoesNotThrow(() -> new MarcaAlojamiento.Fuentes("Playfair Display", "Señorita 2"));
        assertDoesNotThrow(() -> new MarcaAlojamiento.Radios("6px", "12px", "20px"));
        assertDoesNotThrow(() -> conImagen("https://cdn.sga.co/logo.svg"));
        assertDoesNotThrow(() -> conImagen("data:image/svg+xml,%3Csvg%3E"));
    }

    @Test
    void rechazaUnColorQueNoEsRrggbb() {
        assertThrows(ReglaDominioException.class, () -> conAcento("amarillo"));
        assertThrows(ReglaDominioException.class, () -> conAcento("#F4B"));
        assertThrows(ReglaDominioException.class, () -> conAcento("#F4B942; background: url(x)"));
    }

    @Test
    void unaFuenteNoPuedeSalirseDelValorDeCss() {
        assertThrows(ReglaDominioException.class, () -> new MarcaAlojamiento.Fuentes("Mala\"; color: red", null));
        assertThrows(ReglaDominioException.class, () -> new MarcaAlojamiento.Fuentes(null, "x".repeat(61)));
    }

    @Test
    void rechazaUnRadioQueNoEsEnPixeles() {
        assertThrows(ReglaDominioException.class, () -> new MarcaAlojamiento.Radios("1rem", null, null));
        assertThrows(ReglaDominioException.class, () -> new MarcaAlojamiento.Radios(null, "100px", null));
    }

    @Test
    void unaImagenSoloPuedeSerHttpODataImage() {
        assertThrows(ReglaDominioException.class, () -> conImagen("javascript:alert(1)"));
        assertThrows(ReglaDominioException.class, () -> conImagen("data:text/html,x"));
        assertThrows(ReglaDominioException.class, () -> conImagen("https://cdn.sga.co/" + "x".repeat(2000)));
    }

    @Test
    void lasPartesSonObligatoriasAunqueVayanVacias() {
        assertThrows(ReglaDominioException.class, () -> new MarcaAlojamiento(null,
                new MarcaAlojamiento.Fuentes(null, null), new MarcaAlojamiento.Radios(null, null, null), null, null));
    }
}
