package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Cómo se ve la web de reservas de este alojamiento: el mismo esqueleto para todos y la marca de cada uno (DEC-83 ·
 * BL-05). Todo es opcional: lo que falte se queda con el valor neutro de la web. Estos valores terminan en el CSS y en
 * las imágenes de una página pública, por eso solo se aceptan formatos que no pueden salirse de su lugar.
 *
 * @param logoUrl    {@code http(s)} o una imagen embebida ({@code data:image/...})
 * @param portadaUrl foto de la portada; sin ella, la web usa la de un apartamento
 */
public record MarcaAlojamiento(Colores colores, Fuentes fuentes, Radios radio, String logoUrl, String portadaUrl) {

    private static final Pattern COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final Pattern FUENTE = Pattern.compile("[\\p{L}0-9 ]{1,60}");
    private static final Pattern RADIO = Pattern.compile("\\d{1,2}px");
    private static final Pattern IMAGEN = Pattern.compile("(https?://|data:image/)\\S{1,1980}");

    public MarcaAlojamiento {
        if (colores == null || fuentes == null || radio == null) {
            throw new ReglaDominioException("La marca requiere sus colores, fuentes y radios, aunque vayan vacíos");
        }
        exigir(IMAGEN, "una imagen por http(s) o data:image", logoUrl, portadaUrl);
    }

    /** Sin nada propio: la web se ve con sus valores neutros. */
    public static MarcaAlojamiento neutra() {
        return new MarcaAlojamiento(new Colores(null, null, null, null, null, null, null, null, null, null),
                new Fuentes(null, null), new Radios(null, null, null), null, null);
    }

    public boolean esNeutra() {
        return equals(neutra());
    }

    /** Un color por el papel que cumple en la página, en formato {@code #RRGGBB}. */
    public record Colores(String fondo, String superficie, String texto, String textoSuave, String borde,
                          String bordeCampo, String acento, String textoSobreAcento, String peligro, String exito) {

        public Colores {
            exigir(COLOR, "un color #RRGGBB", fondo, superficie, texto, textoSuave, borde, bordeCampo, acento,
                    textoSobreAcento, peligro, exito);
        }
    }

    /** Nombres de familias tipográficas: solo letras, números y espacios, máximo 60. */
    public record Fuentes(String titulos, String texto) {

        public Fuentes {
            exigir(FUENTE, "el nombre de una fuente", titulos, texto);
        }
    }

    /** Radios de las esquinas, en píxeles: {@code 12px}. */
    public record Radios(String pequeno, String mediano, String grande) {

        public Radios {
            exigir(RADIO, "un radio en píxeles", pequeno, mediano, grande);
        }
    }

    private static void exigir(Pattern formato, String esperado, String... valores) {
        Stream.of(valores)
                .filter(v -> v != null && !formato.matcher(v).matches())
                .findFirst()
                .ifPresent(v -> {
                    throw new ReglaDominioException("La marca esperaba " + esperado);
                });
    }
}
