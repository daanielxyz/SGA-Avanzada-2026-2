package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Un ocupante del grupo. El documento es opcional, salvo para el titular, que se reconoce por él (TIT-01).
 *
 * @param tipoDocumento nombre de {@link TipoDocumento}; {@code null} si no trae documento
 */
public record OcupanteCommand(String nombre, LocalDate fechaNacimiento, String tipoDocumento,
                              String numeroDocumento, String nacionalidad) {

    /**
     * Convierte el grupo completo. Los ids los numera la propia reserva en orden ({@code OCU-1}, {@code OCU-2}…),
     * porque solo deben ser únicos dentro de ella (DEC-56).
     */
    static List<Ocupante> aDominio(List<OcupanteCommand> ocupantes) {
        return IntStream.range(0, ocupantes.size())
                .mapToObj(i -> ocupantes.get(i).aDominio(new OcupanteId("OCU-" + (i + 1))))
                .toList();
    }

    private Ocupante aDominio(OcupanteId id) {
        Documento documento = Optional.ofNullable(tipoDocumento)
                .map(tipo -> new Documento(TipoDocumento.valueOf(tipo), numeroDocumento))
                .orElse(null);
        return new Ocupante(id, nombre, fechaNacimiento, documento, nacionalidad);
    }
}
