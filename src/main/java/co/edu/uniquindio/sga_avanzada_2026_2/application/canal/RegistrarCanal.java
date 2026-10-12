package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador registra un canal de venta del alojamiento, que nace activo (CU-54 · CAN-06).
 */
@Service
public class RegistrarCanal {

    private final CanalRepository canales;
    private final AlojamientoRepository alojamientos;
    private final GeneradorCodigos codigos;

    public RegistrarCanal(CanalRepository canales, AlojamientoRepository alojamientos, GeneradorCodigos codigos) {
        this.canales = canales;
        this.alojamientos = alojamientos;
        this.codigos = codigos;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si falta el nombre, o un
     *                                      canal EXTERNO no trae la referencia de su credencial o uno que no lo es
     *                                      la trae (CAN-01 · CAN-02)
     */
    @Transactional
    public CanalResult ejecutar(RegistrarCanalCommand comando) {
        AlojamientoId alojamientoId = new AlojamientoId(comando.alojamientoId());
        alojamientos.buscarPorId(alojamientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", alojamientoId.valor()));

        Canal canal = new Canal(new CanalId(codigos.siguiente(SerieCodigo.CANAL)), alojamientoId, comando.nombre(),
                CanalOrigen.valueOf(comando.tipo()), comando.referenciaCredencial(), true);

        canales.guardar(canal);
        return CanalResult.de(canal);
    }
}
