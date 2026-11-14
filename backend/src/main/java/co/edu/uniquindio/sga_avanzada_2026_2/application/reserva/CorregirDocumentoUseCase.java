package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CorreccionDocumentoDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: recepción corrige el documento mal escrito de un ocupante sin perder sus datos del TRA (#14 · BL-07 ·
 * DEC-120). Si el ocupante es el titular que se aloja, se corrige también el Titular: toca Reserva y Titular en una
 * transacción, para que los dos no queden distintos (excepción documentada en DEC-120).
 */
@Service
public class CorregirDocumentoUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final TitularRepository titulares;
    private final CorreccionDocumentoDomainService correccion;
    private final Clock reloj;

    public CorregirDocumentoUseCase(CargadorReserva cargador, ReservaRepository reservas, TitularRepository titulares,
                                    CorreccionDocumentoDomainService correccion, Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.titulares = titulares;
        this.correccion = correccion;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva ya terminó,
     *         el ocupante no es suyo o no tiene documento, el documento es el mismo o ya es de otro del grupo, o, si
     *         es el titular, ya es de otro titular
     */
    @Transactional
    public ReservaResult ejecutar(CorregirDocumentoCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigo());
        Titular titular = cargador.titular(reserva.titularId());
        Documento nuevo = new Documento(TipoDocumento.valueOf(comando.tipoDocumento()), comando.numeroDocumento());

        correccion.corregirOcupante(reserva, titular, new OcupanteId(comando.ocupanteId()), nuevo, comando.motivo(),
                titulares.buscarPorDocumento(titular.alojamientoId(), nuevo).orElse(null),
                new UsuarioId(comando.autor()), LocalDateTime.now(reloj));

        reservas.guardar(reserva);
        titulares.guardar(titular);
        return ReservaResult.de(reserva);
    }
}
