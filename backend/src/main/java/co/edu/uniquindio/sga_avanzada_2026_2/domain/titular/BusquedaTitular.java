package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * VO: lo que recepción escribe para encontrar a un titular, partido en palabras (DEC-131). Cada palabra debe estar en
 * el nombre completo o en el número de documento; no importan las mayúsculas ni las tildes, y en el documento tampoco
 * los puntos, guiones ni espacios («1.094.000» encuentra «1094000»). Sin palabras, la búsqueda trae a todos.
 *
 * @param palabras en minúsculas y sin tildes, sin repetir
 */
public record BusquedaTitular(List<String> palabras) {

    /** Más que esto no es un nombre ni un documento. */
    public static final int MAXIMO = 100;

    public BusquedaTitular {
        palabras = palabras == null ? List.of() : List.copyOf(palabras);
    }

    /** A partir de lo escrito; nulo o en blanco es «todos». */
    public static BusquedaTitular de(String texto) {
        if (texto == null || texto.isBlank()) {
            return new BusquedaTitular(List.of());
        }
        String limpio = texto.strip();
        if (limpio.length() > MAXIMO) {
            limpio = limpio.substring(0, MAXIMO);
        }
        return new BusquedaTitular(Arrays.stream(normalizar(limpio).split("\\s+"))
                .filter(palabra -> !palabra.isBlank())
                .distinct()
                .toList());
    }

    /** En minúsculas y sin tildes: «Ríos» y «rios» son la misma palabra. */
    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** La palabra como se compara con un número de documento, que se guarda sin separadores. */
    public static String comoDocumento(String palabra) {
        return palabra.replaceAll("[.\\-\\s]", "");
    }

    public boolean todos() {
        return palabras.isEmpty();
    }

    /**
     * Si el titular cumple la búsqueda: cada palabra está en su nombre completo o en su número de documento. La usa
     * quien busca en memoria; la base aplica la misma regla en su consulta.
     */
    public boolean encuentra(String nombreCompleto, String numeroDocumento) {
        String nombre = normalizar(nombreCompleto == null ? "" : nombreCompleto);
        String documento = normalizar(numeroDocumento == null ? "" : numeroDocumento);
        return palabras.stream().allMatch(palabra -> nombre.contains(palabra)
                || (!comoDocumento(palabra).isEmpty() && documento.contains(comoDocumento(palabra))));
    }
}
