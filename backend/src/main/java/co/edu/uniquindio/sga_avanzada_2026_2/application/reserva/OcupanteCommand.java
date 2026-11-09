package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.DatosViaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Un ocupante del grupo. El documento es opcional al reservar; el TRA lo pide antes de la salida (BL-07). Los
 * apellidos pueden llegar aparte o venir dentro de {@code nombres}; el TRA los pide aparte antes de la salida (BL-07).
 *
 * @param tipoDocumento nombre de {@link TipoDocumento}; {@code null} si no trae documento
 * @param principal     si es el huésped principal; si nadie lo es, la reserva toma al primer mayor de edad (DEC-97)
 */
public record OcupanteCommand(String nombres, String primerApellido, String segundoApellido,
                              LocalDate fechaNacimiento, String tipoDocumento, String numeroDocumento,
                              String nacionalidad, boolean principal) {

    /** Sin marcar al huésped principal. */
    public OcupanteCommand(String nombres, String primerApellido, String segundoApellido, LocalDate fechaNacimiento,
                           String tipoDocumento, String numeroDocumento, String nacionalidad) {
        this(nombres, primerApellido, segundoApellido, fechaNacimiento, tipoDocumento, numeroDocumento, nacionalidad,
                false);
    }

    /** Con el nombre en un solo texto, como lo envían los canales externos. */
    public OcupanteCommand(String nombre, LocalDate fechaNacimiento, String tipoDocumento, String numeroDocumento,
                           String nacionalidad) {
        this(nombre, null, null, fechaNacimiento, tipoDocumento, numeroDocumento, nacionalidad);
    }

    /**
     * Convierte el grupo completo. Los ids los numera la propia reserva en orden ({@code OCU-1}, {@code OCU-2}…),
     * porque solo deben ser únicos dentro de ella (DEC-56).
     */
    public static List<Ocupante> aDominio(List<OcupanteCommand> ocupantes) {
        return IntStream.range(0, ocupantes.size())
                .mapToObj(i -> ocupantes.get(i).aDominio(new OcupanteId("OCU-" + (i + 1))))
                .toList();
    }

    private Ocupante aDominio(OcupanteId id) {
        return new Ocupante(id, nombres, primerApellido, segundoApellido, fechaNacimiento,
                documento(tipoDocumento, numeroDocumento), nacionalidad, DatosViaje.VACIOS, principal);
    }

    static Documento documento(String tipoDocumento, String numeroDocumento) {
        return Optional.ofNullable(tipoDocumento)
                .map(tipo -> new Documento(TipoDocumento.valueOf(tipo), numeroDocumento))
                .orElse(null);
    }
}
