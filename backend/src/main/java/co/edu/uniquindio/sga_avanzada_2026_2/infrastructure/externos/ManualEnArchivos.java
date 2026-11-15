package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.ManualAsistente;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.RolConsultante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.SeccionManual;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link ManualAsistente} que lee los archivos de {@code asistente/manual/} una sola vez, al arrancar (DEC-127). Cada
 * archivo empieza con una cabecera entre líneas {@code ---} con {@code id}, {@code titulo}, {@code pantalla} y
 * {@code roles} (separados por comas), y sigue en Markdown. Un archivo mal escrito impide arrancar, para que el error
 * se vea en las pruebas y no en una respuesta.
 */
@Component
public class ManualEnArchivos implements ManualAsistente {

    static final String UBICACION = "classpath*:asistente/manual/*.md";
    private static final String SEPARADOR = "---";

    private final List<SeccionManual> secciones;

    public ManualEnArchivos() {
        this(UBICACION);
    }

    ManualEnArchivos(String ubicacion) {
        this.secciones = leer(ubicacion);
    }

    @Override
    public List<SeccionManual> secciones() {
        return secciones;
    }

    private static List<SeccionManual> leer(String ubicacion) {
        try {
            Resource[] archivos = new PathMatchingResourcePatternResolver().getResources(ubicacion);
            return Arrays.stream(archivos)
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .map(ManualEnArchivos::seccion)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el manual del asistente en " + ubicacion, e);
        }
    }

    private static SeccionManual seccion(Resource archivo) {
        String texto;
        try {
            texto = archivo.getContentAsString(StandardCharsets.UTF_8).replace("\r\n", "\n").strip();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo.getFilename(), e);
        }
        return analizar(archivo.getFilename(), texto);
    }

    static SeccionManual analizar(String nombre, String texto) {
        int fin = texto.indexOf("\n" + SEPARADOR, SEPARADOR.length());
        if (!texto.startsWith(SEPARADOR) || fin < 0) {
            throw new IllegalStateException("El archivo " + nombre + " del manual no tiene su cabecera entre ---");
        }
        Map<String, String> cabecera = new HashMap<>();
        for (String linea : texto.substring(SEPARADOR.length(), fin).strip().split("\n")) {
            int dosPuntos = linea.indexOf(':');
            if (dosPuntos > 0) {
                cabecera.put(linea.substring(0, dosPuntos).strip(), sinComillas(linea.substring(dosPuntos + 1)));
            }
        }
        return new SeccionManual(cabecera.get("id"), cabecera.get("titulo"), cabecera.get("pantalla"),
                roles(nombre, cabecera.getOrDefault("roles", "")), texto.substring(fin + SEPARADOR.length() + 1).strip());
    }

    private static Set<RolConsultante> roles(String nombre, String roles) {
        try {
            return Arrays.stream(roles.split(","))
                    .map(String::strip)
                    .filter(rol -> !rol.isEmpty())
                    .map(RolConsultante::valueOf)
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("El archivo " + nombre + " del manual tiene un rol que no existe: " + roles,
                    e);
        }
    }

    private static String sinComillas(String valor) {
        String limpio = valor.strip();
        return limpio.length() >= 2 && limpio.startsWith("\"") && limpio.endsWith("\"")
                ? limpio.substring(1, limpio.length() - 1) : limpio;
    }
}
