package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: la próxima noche libre de cada apartamento activo, para mostrar sus precios antes de que el visitante
 * elija fechas (CU-07). No reserva ni guarda nada.
 */
@Service
public class BuscarProximasNochesLibresUseCase {

    private final CargadorReserva cargador;
    private final ApartamentoRepository apartamentos;
    private final ReservaRepository reservas;
    private final DisponibilidadDomainService disponibilidad;

    public BuscarProximasNochesLibresUseCase(CargadorReserva cargador, ApartamentoRepository apartamentos,
                                             ReservaRepository reservas, DisponibilidadDomainService disponibilidad) {
        this.cargador = cargador;
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.disponibilidad = disponibilidad;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si el
     *         alojamiento no existe
     */
    @Transactional(readOnly = true)
    public List<ProximaNocheLibreResult> ejecutar(BuscarProximasNochesLibresCommand comando) {
        Alojamiento alojamiento = cargador.alojamiento(new AlojamientoId(comando.alojamientoId()));

        return apartamentos.buscarActivos(alojamiento.id()).stream()
                .map(a -> new ProximaNocheLibreResult(a.codigo().valor(), disponibilidad.proximaNocheLibre(a,
                        comando.desde(), comando.dias(), reservas.buscarActivasPorApartamento(a.codigo()),
                        alojamiento.parametros()).map(Noche::fecha).orElse(null)))
                .toList();
    }
}
