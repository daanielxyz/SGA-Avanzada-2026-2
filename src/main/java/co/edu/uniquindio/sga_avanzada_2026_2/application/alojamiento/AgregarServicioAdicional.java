package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador agrega un servicio adicional al catálogo del alojamiento (CU-30 · SERV-01).
 */
@Service
public class AgregarServicioAdicional {

    private final AlojamientoRepository alojamientos;
    private final GeneradorCodigos codigos;

    public AgregarServicioAdicional(AlojamientoRepository alojamientos, GeneradorCodigos codigos) {
        this.alojamientos = alojamientos;
        this.codigos = codigos;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si falta el nombre o
     *                                      el valor es negativo (SERV-01 · DIN-03)
     */
    @Transactional
    public AlojamientoResult ejecutar(AgregarServicioAdicionalCommand comando) {
        AlojamientoId id = new AlojamientoId(comando.alojamientoId());
        Alojamiento alojamiento = alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));

        alojamiento.agregarServicioAdicional(new ServicioAdicional(
                new ServicioAdicionalId(codigos.siguiente(SerieCodigo.SERVICIO)), comando.nombre(),
                comando.generaCargo(), new Dinero(comando.valor()), true));

        alojamientos.guardar(alojamiento);
        return AlojamientoResult.de(alojamiento);
    }
}
