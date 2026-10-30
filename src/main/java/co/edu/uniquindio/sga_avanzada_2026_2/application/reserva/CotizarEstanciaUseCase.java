package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Caso de uso: calcular el valor de una estancia con su desglose noche por noche, antes de reservar (CU-09). No
 * reserva ni guarda nada (COT-01).
 */
@Service
public class CotizarEstanciaUseCase {

    private final CargadorReserva cargador;
    private final TarifaRepository tarifas;
    private final TarificacionDomainService tarificacion;
    private final Clock reloj;

    public CotizarEstanciaUseCase(CargadorReserva cargador, TarifaRepository tarifas,
                                  TarificacionDomainService tarificacion, Clock reloj) {
        this.cargador = cargador;
        this.tarifas = tarifas;
        this.tarificacion = tarificacion;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe el apartamento, su alojamiento o el calendario
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la estancia es
     *         inválida (RN-03) o falta la tarifa de alguna noche (RN-05 · TAR-03)
     */
    @Transactional(readOnly = true)
    public CotizacionResult ejecutar(CotizarEstanciaCommand comando) {
        Apartamento apartamento = cargador.apartamento(comando.apartamento());
        Alojamiento alojamiento = cargador.alojamientoDe(apartamento);
        Estancia estancia = new Estancia(comando.entrada(), comando.salida());

        return CotizacionResult.de(tarificacion.calcularValorEstancia(apartamento.codigo(), estancia,
                ocupantes(comando.fechasNacimiento()), alojamiento.parametros().umbralEdadFacturable(),
                cargador.calendario(alojamiento.id()),
                tarifas.buscarVigentesPorApartamento(apartamento.codigo(), LocalDate.now(reloj))));
    }

    /**
     * Lo mismo sabiendo solo cuántos pagan, sin fechas de nacimiento (RN-06): el portal muestra el precio antes de
     * pedir los datos de cada ocupante, que llegan al reservar.
     *
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe el apartamento, su alojamiento o el calendario
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la estancia es
     *         inválida (RN-03), los que pagan son menos de cero o falta la tarifa de alguna noche (RN-05 · TAR-03)
     */
    @Transactional(readOnly = true)
    public CotizacionResult ejecutar(CotizarPorCantidadCommand comando) {
        Apartamento apartamento = cargador.apartamento(comando.apartamento());
        Alojamiento alojamiento = cargador.alojamientoDe(apartamento);
        Estancia estancia = new Estancia(comando.entrada(), comando.salida());

        return CotizacionResult.de(tarificacion.calcularValorEstancia(apartamento.codigo(), estancia,
                comando.facturables(), cargador.calendario(alojamiento.id()),
                tarifas.buscarVigentesPorApartamento(apartamento.codigo(), LocalDate.now(reloj))));
    }

    // Para cotizar solo cuenta la edad a la fecha de entrada (RN-06); el nombre es de relleno
    private static List<Ocupante> ocupantes(List<LocalDate> fechasNacimiento) {
        return IntStream.range(0, fechasNacimiento.size())
                .mapToObj(i -> new Ocupante(new OcupanteId("OCU-" + (i + 1)), "Ocupante " + (i + 1),
                        fechasNacimiento.get(i), null, null))
                .toList();
    }
}
