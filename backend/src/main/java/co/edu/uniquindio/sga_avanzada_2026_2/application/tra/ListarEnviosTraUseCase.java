package co.edu.uniquindio.sga_avanzada_2026_2.application.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EnvioTraRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EstadoEnvioTra;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: los envíos del TRA de un estado, para que ninguno con error se pierda (DEC-130). */
@Service
public class ListarEnviosTraUseCase {

    private final EnvioTraRepository envios;

    public ListarEnviosTraUseCase(EnvioTraRepository envios) {
        this.envios = envios;
    }

    /** @param estado nombre de {@link EstadoEnvioTra}; el formato lo valida el borde (400) */
    @Transactional(readOnly = true)
    public Pagina<EnvioTraResult> ejecutar(String estado, int numeroPagina) {
        return envios.listarPorEstado(EstadoEnvioTra.valueOf(estado), numeroPagina).map(EnvioTraResult::de);
    }
}
