package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.CorreccionDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.DeclaracionNoShow;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso: ver el detalle de una reserva con el nombre de su titular, el anticipo exigido, quién declaró el
 * no-show y los documentos corregidos (CU-49 · RES-15 · BL-25 · DEC-120).
 */
@Service
public class ConsultarReservaConTitularUseCase {

    private final CargadorReserva cargador;
    private final TitularRepository titulares;
    private final DirectorioPersonal personal;

    public ConsultarReservaConTitularUseCase(CargadorReserva cargador, TitularRepository titulares,
                                             DirectorioPersonal personal) {
        this.cargador = cargador;
        this.titulares = titulares;
        this.personal = personal;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public ReservaConTitularResult ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        return new ReservaConTitularResult(ReservaResult.de(reserva),
                titulares.buscarPorId(reserva.titularId()).map(Titular::nombre).orElse(null),
                reserva.anticipoExigido(cargador.alojamientoDe(reserva).parametros().anticipo()).monto(),
                Optional.ofNullable(reserva.declaracionNoShow()).map(this::declaracion).orElse(null),
                reserva.correccionesDocumento().stream().map(this::correccion).toList());
    }

    private ReservaConTitularResult.CorreccionDocumentoResult correccion(CorreccionDocumento c) {
        return new ReservaConTitularResult.CorreccionDocumentoResult(c.ocupante(), c.anterior().tipo().name(),
                c.anterior().numero(), c.nuevo().tipo().name(), c.nuevo().numero(), c.motivo(), c.autor().valor(),
                personal.nombreDe(c.autor()).orElse(null), c.fechaHora(), c.titular());
    }

    private ReservaConTitularResult.DeclaracionNoShowResult declaracion(DeclaracionNoShow declaracion) {
        return new ReservaConTitularResult.DeclaracionNoShowResult(declaracion.autor().valor(),
                personal.nombreDe(declaracion.autor()).orElse(null), declaracion.fechaHora());
    }
}
