package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.util.Set;

/**
 * VO: una sección del manual del PMS (DEC-127): un archivo de {@code asistente/manual/} con su cabecera.
 *
 * @param id        nombre corto, p. ej. {@code reservas-llegada}; es lo que el asistente cita como fuente
 * @param pantalla  la ruta del PMS, p. ej. {@code /reservas/{codigo}/llegada}
 * @param roles     quiénes la pueden leer
 * @param contenido el texto en Markdown, sin la cabecera
 */
public record SeccionManual(String id, String titulo, String pantalla, Set<RolConsultante> roles, String contenido) {

    // DEC-127
    public SeccionManual {
        if (id == null || id.isBlank() || titulo == null || titulo.isBlank() || contenido == null
                || contenido.isBlank()) {
            throw new ReglaDominioException("Una sección del manual necesita id, título y contenido");
        }
        if (roles == null || roles.isEmpty()) {
            throw new ReglaDominioException("La sección " + id + " del manual no dice qué roles la leen");
        }
        roles = Set.copyOf(roles);
        pantalla = pantalla == null ? "" : pantalla;
    }

    /** Si quien pregunta puede leerla: a servicio nunca le llega lo de reservas (DEC-127). */
    public boolean esPara(RolConsultante rol) {
        return roles.contains(rol);
    }
}
