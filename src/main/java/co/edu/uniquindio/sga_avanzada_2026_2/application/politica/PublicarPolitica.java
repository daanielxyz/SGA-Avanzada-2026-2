package co.edu.uniquindio.sga_avanzada_2026_2.application.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: el administrador publica una versión nueva de la política de cancelación del alojamiento (CU-35).
 * Las reservas ya creadas conservan la versión que congelaron (POL-04).
 */
@Service
public class PublicarPolitica {

    private final PoliticaCancelacionRepository politicas;
    private final AlojamientoRepository alojamientos;
    private final GeneradorCodigos codigos;

    public PublicarPolitica(PoliticaCancelacionRepository politicas, AlojamientoRepository alojamientos,
                            GeneradorCodigos codigos) {
        this.politicas = politicas;
        this.alojamientos = alojamientos;
        this.codigos = codigos;
    }

    /**
     * Crea la versión siguiente a la vigente, o la primera si el alojamiento aún no tiene (DEC-42).
     *
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si hay menos tramos
     *                                      que el mínimo del alojamiento (POL-01) o no cubren toda antelación (POL-02)
     */
    @Transactional
    public PoliticaResult ejecutar(PublicarPoliticaCommand comando) {
        AlojamientoId alojamientoId = new AlojamientoId(comando.alojamientoId());
        Alojamiento alojamiento = alojamientos.buscarPorId(alojamientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", alojamientoId.valor()));
        PoliticaId id = new PoliticaId(codigos.siguiente(SerieCodigo.POLITICA));
        List<TramoCancelacion> tramos = comando.tramosDominio();
        Penalizacion noShow = comando.penalizacionNoShow().aDominio();
        int minimoTramos = alojamiento.parametros().minimoTramosCancelacion();

        PoliticaCancelacion nueva = politicas.buscarVigente(alojamientoId)
                .map(vigente -> vigente.nuevaVersion(id, tramos, noShow, minimoTramos))
                .orElseGet(() -> PoliticaCancelacion.crear(id, alojamientoId, tramos, noShow, minimoTramos));

        politicas.guardar(nueva);
        return PoliticaResult.de(nueva);
    }
}
