package co.edu.uniquindio.sga_avanzada_2026_2.application.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EnvioTraRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso del planificador: las reservas cuyo TRA falta enviar (DEC-130). Las que fallaron se reintentan solas
 * hasta {@code sga.tra.intentos-automaticos} veces; después, solo con «Reintentar».
 */
@Service
public class BuscarEnviosTraPendientesUseCase {

    private final EnvioTraRepository envios;
    private final int intentosAutomaticos;

    public BuscarEnviosTraPendientesUseCase(EnvioTraRepository envios,
                                            @Value("${sga.tra.intentos-automaticos:12}") int intentosAutomaticos) {
        this.envios = envios;
        this.intentosAutomaticos = intentosAutomaticos;
    }

    @Transactional(readOnly = true)
    public List<String> ejecutar() {
        return envios.buscarParaEnviar(intentosAutomaticos).stream().map(ReservaId::valor).toList();
    }
}
