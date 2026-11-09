package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.DatosViaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Genero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.MotivoViaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso: recepción completa los datos del TRA de un ocupante, en la llegada o después (BL-07). Solo cambia la
 * Reserva; la salida queda bloqueada mientras falte alguno.
 */
@Service
public class CompletarDatosOcupanteUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;

    public CompletarDatosOcupanteUseCase(CargadorReserva cargador, ReservaRepository reservas) {
        this.cargador = cargador;
        this.reservas = reservas;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva ya terminó,
     *         el ocupante no es suyo, llega otro documento o el ocupante queda repetido (BL-07 · OCU-05)
     */
    @Transactional
    public ReservaResult ejecutar(CompletarDatosOcupanteCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigo());

        reserva.completarDatosOcupante(new OcupanteId(comando.ocupanteId()), comando.nombres(),
                comando.primerApellido(), comando.segundoApellido(),
                OcupanteCommand.documento(comando.tipoDocumento(), comando.numeroDocumento()), comando.nacionalidad(),
                new DatosViaje(Optional.ofNullable(comando.genero()).map(Genero::valueOf).orElse(null),
                        comando.paisResidencia(), comando.ciudadResidencia(), comando.ciudadProcedencia(),
                        comando.ciudadDestino(),
                        Optional.ofNullable(comando.motivoViaje()).map(MotivoViaje::valueOf).orElse(null)));

        reservas.guardar(reserva);
        return ReservaResult.de(reserva);
    }
}
