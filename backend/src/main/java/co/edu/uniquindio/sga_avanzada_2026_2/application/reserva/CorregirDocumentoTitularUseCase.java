package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
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
 * Caso de uso: recepción corrige el documento mal escrito del titular de una reserva, también si no se aloja (#14 ·
 * DEC-120). Sigue siendo el mismo titular, con sus reservas y su cuenta; si se aloja, su ocupante se corrige con él.
 * Toca Reserva y Titular en una transacción (excepción documentada en DEC-120).
 */
@Service
public class CorregirDocumentoTitularUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final TitularRepository titulares;
    private final CorreccionDocumentoDomainService correccion;
    private final Clock reloj;

    public CorregirDocumentoTitularUseCase(CargadorReserva cargador, ReservaRepository reservas,
                                           TitularRepository titulares, CorreccionDocumentoDomainService correccion,
                                           Clock reloj) {
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
     *         el documento es el mismo, es un registro civil, ya es de otro titular o de otro del grupo
     */
    @Transactional
    public ReservaResult ejecutar(CorregirDocumentoTitularCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigo());
        Titular titular = cargador.titular(reserva.titularId());
        Documento nuevo = new Documento(TipoDocumento.valueOf(comando.tipoDocumento()), comando.numeroDocumento());

        correccion.corregirTitular(reserva, titular, nuevo, comando.motivo(),
                titulares.buscarPorDocumento(titular.alojamientoId(), nuevo).orElse(null),
                new UsuarioId(comando.autor()), LocalDateTime.now(reloj));

        reservas.guardar(reserva);
        titulares.guardar(titular);
        return ReservaResult.de(reserva);
    }
}
