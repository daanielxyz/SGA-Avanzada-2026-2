package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;

import java.time.LocalDateTime;

/**
 * Servicio de dominio sin estado: corrige un documento mal escrito en la reserva y, si es el del titular, también en
 * el Titular, para que los dos no queden distintos (#14 · DEC-120). Cruza Reserva y Titular, que recibe por
 * parámetro.
 */
public class CorreccionDocumentoDomainService {

    /**
     * Corrige el documento de un ocupante. Si ese ocupante es el titular que se aloja, corrige también al titular.
     *
     * @param duenoDelNuevo el titular del alojamiento que ya tiene el documento nuevo; nulo si no hay
     * @throws ReglaDominioException las de {@code Reserva.corregirDocumento} y, si es el titular, las de
     *                               {@link #corregirTitular}
     */
    public void corregirOcupante(Reserva reserva, Titular titular, OcupanteId ocupanteId, Documento nuevo,
                                 String motivo, Titular duenoDelNuevo, UsuarioId autor, LocalDateTime ahora) {
        if (reserva.esElTitular(ocupanteId, titular)) {
            corregirTitular(reserva, titular, nuevo, motivo, duenoDelNuevo, autor, ahora);
            return;
        }
        reserva.corregirDocumento(ocupanteId, nuevo, motivo, autor, ahora);
    }

    /**
     * Corrige el documento del titular de la reserva y, si se aloja, el de su ocupante. Sigue siendo el mismo
     * titular en todas sus reservas; en las demás, su ocupante conserva el documento que tenía.
     *
     * @param duenoDelNuevo el titular del alojamiento que ya tiene el documento nuevo; nulo si no hay
     * @throws ReglaDominioException si el documento nuevo ya es de otro titular del alojamiento (TIT-05), el titular
     *                               no es el de la reserva, la reserva ya terminó, el documento es el mismo, ya lo
     *                               tiene otro del grupo (OCU-05), es un registro civil (TIT-01) o falta el motivo
     */
    public void corregirTitular(Reserva reserva, Titular titular, Documento nuevo, String motivo,
                                Titular duenoDelNuevo, UsuarioId autor, LocalDateTime ahora) {
        if (duenoDelNuevo != null && !duenoDelNuevo.equals(titular)) {
            throw new ReglaDominioException("El documento " + nuevo.tipo().name() + " " + nuevo.numero()
                    + " ya es de otro titular del alojamiento, " + duenoDelNuevo.nombre()
                    + ": si es esa persona, cambia el titular de la reserva");
        }
        reserva.corregirDocumentoDelTitular(titular, nuevo, motivo, autor, ahora);
        titular.corregirDocumento(nuevo);
    }
}
