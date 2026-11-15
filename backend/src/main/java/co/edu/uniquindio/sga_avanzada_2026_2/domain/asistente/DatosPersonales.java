package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import java.util.regex.Pattern;

/**
 * Quita de un texto lo que tiene forma de dato personal antes de que salga a un servicio externo (DEC-127): correos,
 * celulares y documentos (cédulas con o sin puntos, pasaportes). Los códigos del sistema (RES-2026-00042, APT-101,
 * TIT-12), las fechas, las horas y el dinero con «$» o «pesos» se conservan, porque sin ellos el asistente no
 * entiende la pregunta. Los nombres no se pueden distinguir de otras palabras: por eso el panel pide no escribirlos.
 */
public final class DatosPersonales {

    public static final String CORREO = "[correo]";
    public static final String TELEFONO = "[teléfono]";
    public static final String DOCUMENTO = "[documento]";

    // Antes y después de un número no puede haber una letra, un dígito, un guion ni un «$» (así se respetan los
    // códigos del sistema, las fechas AAAA-MM-DD y el dinero); el final tampoco puede ser una palabra de dinero
    private static final String ANTES = "(?<![\\p{L}\\d$-])(?<!\\$\\s)";
    private static final String DESPUES = "(?![\\p{L}\\d-])(?!\\s?(?i:pesos|mil|cop)\\b)";

    private static final Pattern CORREOS = Pattern.compile("[\\p{L}\\d._%+-]+@[\\p{L}\\d-]+(\\.[\\p{L}\\d-]+)+");
    // Celular de Colombia (3xx xxx xxxx), con o sin indicativo y con espacios o guiones
    private static final Pattern CELULARES = Pattern.compile(
            ANTES + "(\\+\\d{1,3}[\\s-]?)?3\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{4}" + DESPUES);
    // Pasaporte: una a tres letras seguidas de seis a nueve dígitos
    private static final Pattern PASAPORTES = Pattern.compile("(?<![\\p{L}\\d-])\\p{Lu}{1,3}\\d{6,9}(?![\\p{L}\\d-])");
    // Cédula con puntos o espacios de miles y al menos siete dígitos (1.094.123.456); «500.000» queda, puede ser dinero
    private static final Pattern DOCUMENTOS_CON_PUNTOS = Pattern.compile(
            ANTES + "\\d{1,3}(?:[.\\s]\\d{3}){2,3}" + DESPUES);
    // Seis o más dígitos seguidos
    private static final Pattern DOCUMENTOS = Pattern.compile(ANTES + "\\d{6,15}" + DESPUES);

    private DatosPersonales() {
    }

    /**
     * @return el texto con cada dato personal cambiado por su marca; {@code null} si el texto es nulo
     */
    // DEC-127
    public static String enmascarar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = CORREOS.matcher(texto).replaceAll(CORREO);
        limpio = CELULARES.matcher(limpio).replaceAll(TELEFONO);
        limpio = PASAPORTES.matcher(limpio).replaceAll(DOCUMENTO);
        limpio = DOCUMENTOS_CON_PUNTOS.matcher(limpio).replaceAll(DOCUMENTO);
        return DOCUMENTOS.matcher(limpio).replaceAll(DOCUMENTO);
    }
}
