package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador cambia el rango de una temporada específica (CU-33). Las reservas ya creadas no
 * cambian porque su valor está congelado (TEM-05).
 */
@Service
public class CambiarFechasTemporada {

    private final CalendarioTemporadasRepository calendarios;

    public CambiarFechasTemporada(CalendarioTemporadasRepository calendarios) {
        this.calendarios = calendarios;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no tiene calendario
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la temporada no
     *         existe, es la base o el rango nuevo es inválido o se solapa (TEM-01 · TEM-02)
     */
    @Transactional
    public CalendarioResult ejecutar(CambiarFechasTemporadaCommand comando) {
        AlojamientoId alojamientoId = new AlojamientoId(comando.alojamientoId());
        CalendarioTemporadas calendario = calendarios.buscarPorAlojamiento(alojamientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el calendario de temporadas del alojamiento",
                        alojamientoId.valor()));

        calendario.cambiarFechas(new TemporadaId(comando.temporadaId()), comando.fechaInicio(), comando.fechaFin());

        calendarios.guardar(calendario);
        return CalendarioResult.de(calendario);
    }
}
