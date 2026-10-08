package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso: recepción cambia fechas, ocupantes o apartamento de una reserva que no ha iniciado (CU-17 · RN-14).
 * Recalcula con las tarifas vigentes hoy (TAR-06) y registra la diferencia como ajuste en el folio, en la misma
 * transacción (DEC-59). La política congelada no cambia.
 */
@Service
public class ModificarReserva {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final TarifaRepository tarifas;
    private final DisponibilidadDomainService disponibilidad;
    private final TarificacionDomainService tarificacion;
    private final Clock reloj;

    public ModificarReserva(CargadorReserva cargador, ReservaRepository reservas, FolioRepository folios,
                            TarifaRepository tarifas, DisponibilidadDomainService disponibilidad,
                            TarificacionDomainService tarificacion, Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.folios = folios;
        this.tarifas = tarifas;
        this.disponibilidad = disponibilidad;
        this.tarificacion = tarificacion;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su folio, su titular o el apartamento destino
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva ya
     *         inició o terminó, el destino no está disponible sin contar esta reserva (RN-14), falta una tarifa
     *         (RN-05) o los nuevos datos violan una regla de creación (RN-02 · RN-04 · OCU-02 · RP-01 · TIT-01)
     */
    @Transactional
    public ReservaResult ejecutar(ModificarReservaCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigo());
        Folio folio = cargador.folioDe(reserva);
        Apartamento destino = cargador.apartamento(comando.apartamento());
        Alojamiento alojamiento = cargador.alojamientoDe(destino);
        ParametrosAlojamiento parametros = alojamiento.parametros();
        CalendarioTemporadas calendario = cargador.calendario(alojamiento.id());
        LocalDate hoy = LocalDate.now(reloj);
        Estancia estancia = new Estancia(comando.entrada(), comando.salida());
        List<Ocupante> ocupantes = OcupanteCommand.aDominio(comando.ocupantes());
        Dinero valorAnterior = reserva.valorTotal();

        disponibilidad.verificarParaModificar(reserva, destino, estancia, ocupantes.size(),
                reservas.buscarActivasPorApartamento(destino.codigo()), parametros).exigir();
        Cotizacion cotizacion = tarificacion.calcularValorEstancia(destino.codigo(), estancia, ocupantes,
                parametros.umbralEdadFacturable(), calendario,
                tarifas.buscarVigentesPorApartamento(destino.codigo(), hoy));
        reserva.modificar(estancia, ocupantes, destino.codigo(), cotizacion, destino.capacidad(),
                calendario.estanciaMinimaPara(estancia), cargador.titular(reserva.titularId()),
                parametros.umbralEdadFacturable(), hoy);
        folio.ajustarPorModificacion(valorAnterior, reserva.valorTotal(), hoy);

        reservas.guardar(reserva);
        folios.guardar(folio);
        return ReservaResult.de(reserva);
    }
}
