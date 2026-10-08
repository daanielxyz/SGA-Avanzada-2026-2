package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador retira una temporada específica (eliminación lógica, TEM-06); sus fechas vuelven a
 * la base (TEM-11).
 */
@Service
public class DesactivarTemporada {

    private final CalendarioTemporadasRepository calendarios;

    public DesactivarTemporada(CalendarioTemporadasRepository calendarios) {
        this.calendarios = calendarios;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no tiene calendario
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la temporada no
     *                                      existe, ya estaba inactiva o es la base (TEM-10)
     */
    @Transactional
    public CalendarioResult ejecutar(DesactivarTemporadaCommand comando) {
        AlojamientoId alojamientoId = new AlojamientoId(comando.alojamientoId());
        CalendarioTemporadas calendario = calendarios.buscarPorAlojamiento(alojamientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el calendario de temporadas del alojamiento",
                        alojamientoId.valor()));

        calendario.desactivarTemporada(new TemporadaId(comando.temporadaId()));

        calendarios.guardar(calendario);
        return CalendarioResult.de(calendario);
    }
}
