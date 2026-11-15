package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.util.List;
import java.util.Optional;

/**
 * VO: lo que responde el asistente (DEC-127).
 *
 * @param fuentes los ids de las secciones del manual en que se basa; vacía si no encontró nada
 * @param motivo  solo si salió del manual: por qué no respondió la IA
 */
public record Respuesta(String texto, List<String> fuentes, OrigenRespuesta origen, MotivoSinIa motivo) {

    // DEC-127
    public Respuesta {
        if (texto == null || texto.isBlank() || origen == null) {
            throw new ReglaDominioException("La respuesta del asistente necesita su texto y su origen");
        }
        if ((origen == OrigenRespuesta.LOCAL) != (motivo != null)) {
            throw new ReglaDominioException("Solo una respuesta del manual dice por qué no respondió la IA");
        }
        fuentes = fuentes == null ? List.of() : fuentes.stream().distinct().toList();
    }

    public static Respuesta deIa(String texto, List<String> fuentes) {
        return new Respuesta(texto, fuentes, OrigenRespuesta.IA, null);
    }

    public static Respuesta delManual(String texto, List<String> fuentes, MotivoSinIa motivo) {
        return new Respuesta(texto, fuentes, OrigenRespuesta.LOCAL, motivo);
    }

    public Optional<MotivoSinIa> motivoSinIa() {
        return Optional.ofNullable(motivo);
    }
}
