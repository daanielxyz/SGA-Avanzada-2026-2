package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Responde con el manual, sin IA (DEC-127): busca la sección del rol que más palabras comparte con la pregunta y la
 * devuelve entera. Es la respuesta cuando la IA está apagada o no puede responder, así que el asistente nunca deja a
 * nadie sin ayuda. Sin estado: recibe todo en la {@link Consulta}.
 */
public class RespuestaLocalDomainService {

    static final String SIN_COINCIDENCIA = "No encontré eso en el manual del PMS. Pregúntale al administrador.";

    // Palabras que no ayudan a elegir la sección
    private static final Set<String> VACIAS = Set.of("como", "cual", "cuales", "cuando", "donde", "para", "porque",
            "pero", "esta", "este", "esto", "estos", "estas", "tiene", "tengo", "hago", "hace", "hacer", "puedo",
            "puede", "quiero", "necesito", "sobre", "entre", "desde", "hasta", "cuanto", "cuanta", "otra", "otro",
            "todo", "todos", "nada", "solo", "tambien", "ahora", "favor", "ayuda", "sistema", "pms", "que", "los",
            "las", "del", "una", "uno", "con", "sin", "por", "mas", "muy", "hay", "ser", "son", "fue", "dice", "sale");
    private static final Pattern NO_LETRAS = Pattern.compile("[^\\p{L}\\d]+");
    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    private static final int RAIZ = 5;
    private static final int PESO_TITULO = 3;
    private static final int PESO_PANTALLA = 2;

    /**
     * @param motivo por qué responde el manual y no la IA
     */
    // DEC-127
    public Respuesta responder(Consulta consulta, MotivoSinIa motivo) {
        Set<String> buscadas = raices(consulta.pregunta().texto());
        return consulta.secciones().stream()
                .map(seccion -> new Puntaje(seccion, puntaje(seccion, buscadas, consulta.pantalla())))
                .filter(puntaje -> puntaje.valor() > 0)
                .max(Comparator.comparingInt(Puntaje::valor))
                .map(mejor -> Respuesta.delManual(textoDe(mejor.seccion()), List.of(mejor.seccion().id()), motivo))
                .orElseGet(() -> Respuesta.delManual(SIN_COINCIDENCIA, List.of(), motivo));
    }

    private static int puntaje(SeccionManual seccion, Set<String> buscadas, String pantalla) {
        Set<String> titulo = raices(seccion.titulo());
        Set<String> contenido = raices(seccion.contenido());
        int valor = 0;
        for (String raiz : buscadas) {
            if (titulo.contains(raiz)) {
                valor += PESO_TITULO;
            } else if (contenido.contains(raiz)) {
                valor++;
            }
        }
        // Desempata a favor de la sección de la pantalla donde está la persona, solo si ya coincide en algo
        return valor > 0 && esLaPantalla(seccion.pantalla(), pantalla) ? valor + PESO_PANTALLA : valor;
    }

    /** «/reservas/{codigo}/llegada» coincide con «/reservas/RES-2026-00042/llegada». */
    private static boolean esLaPantalla(String plantilla, String pantalla) {
        if (plantilla.isBlank() || pantalla.isBlank() || "/".equals(plantilla)) {
            return false;
        }
        String patron = Pattern.quote(plantilla).replace("{codigo}", "\\E[^/]+\\Q");
        return Pattern.matches(patron, pantalla);
    }

    private static String textoDe(SeccionManual seccion) {
        return "Esto dice el manual en «" + seccion.titulo() + "»:\n\n" + seccion.contenido().strip();
    }

    /** Las primeras letras de cada palabra con sentido, sin tildes: «cancelación» y «cancelar» dan «cance». */
    static Set<String> raices(String texto) {
        String sinTildes = MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        Set<String> raices = new HashSet<>();
        for (String palabra : NO_LETRAS.split(sinTildes.toLowerCase(Locale.ROOT))) {
            if (palabra.length() >= 3 && !VACIAS.contains(palabra)) {
                raices.add(palabra.length() > RAIZ ? palabra.substring(0, RAIZ) : palabra);
            }
        }
        return raices;
    }

    private record Puntaje(SeccionManual seccion, int valor) {
    }
}
