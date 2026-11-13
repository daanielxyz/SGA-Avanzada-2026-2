package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CupoParqueaderoDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Caso de uso: recepción cambia solo los vehículos y las mascotas de una reserva que no ha llegado, por ejemplo en la
 * llegada, cuando vino sin la mascota (R3 · BL-23 · DEC-116). A diferencia de modificar (DEC-106), las noches no se
 * vuelven a cotizar: conservan la tarifa con que se reservó. Los extras se calculan con los cargos vigentes y la
 * diferencia entra al folio como ajuste, en la misma transacción (DEC-44).
 */
@Service
public class CambiarExtrasReservaUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final CupoParqueaderoDomainService cupoParqueadero;
    private final Clock reloj;

    public CambiarExtrasReservaUseCase(CargadorReserva cargador, ReservaRepository reservas, FolioRepository folios,
                                       CupoParqueaderoDomainService cupoParqueadero, Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.folios = folios;
        this.cupoParqueadero = cupoParqueadero;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su folio o su apartamento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva ya llegó
     *         o terminó, las mascotas pasan el límite del apartamento (RP-03) o los vehículos no caben en el
     *         parqueadero (RP-02)
     */
    @Transactional
    public ReservaResult ejecutar(CambiarExtrasCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigo());
        Folio folio = cargador.folioDe(reserva);
        Apartamento apartamento = cargador.apartamento(reserva.apartamentoId());
        ParametrosAlojamiento parametros = cargador.alojamientoDe(apartamento).parametros();
        Dinero extrasAnteriores = reserva.extras().total();

        // DEC-116: solo antes de la llegada; el valor de las noches no se toca
        reserva.cambiarExtras(comando.carros(), comando.motos(), comando.mascotas(),
                parametros.calcularExtras(comando.carros(), comando.motos(), comando.mascotas(),
                        reserva.estancia().noches().size()));
        // RP-03 · DEC-93: las mascotas, contra el límite del apartamento
        parametros.exigirMascotas(comando.mascotas(), apartamento);
        // RP-02: los vehículos deben caber en el parqueadero, sin contar dos veces esta reserva
        cupoParqueadero.exigirCupo(reserva.estancia(), reserva.vehiculos(), reservas.buscarActivasConVehiculos(),
                reserva, parametros.cupoParqueadero());
        folio.ajustarPorCambioDeExtras(extrasAnteriores, reserva.extras().total(), LocalDate.now(reloj));

        reservas.guardar(reserva);
        folios.guardar(folio);
        return ReservaResult.de(reserva);
    }
}
