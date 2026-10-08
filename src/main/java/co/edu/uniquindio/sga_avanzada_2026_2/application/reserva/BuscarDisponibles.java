package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: buscar los apartamentos libres para unas fechas y un grupo (CU-07). Solo mira los activos: los
 * retirados no aparecen (APA-16). Consultar no reserva nada (DISP-06).
 */
@Service
public class BuscarDisponibles {

    private final CargadorReserva cargador;
    private final ApartamentoRepository apartamentos;
    private final ReservaRepository reservas;
    private final DisponibilidadDomainService disponibilidad;

    public BuscarDisponibles(CargadorReserva cargador, ApartamentoRepository apartamentos,
                             ReservaRepository reservas, DisponibilidadDomainService disponibilidad) {
        this.cargador = cargador;
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.disponibilidad = disponibilidad;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si el
     *         alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la estancia es
     *         inválida (RN-03)
     */
    @Transactional(readOnly = true)
    public List<ApartamentoDisponibleResult> ejecutar(BuscarDisponiblesCommand comando) {
        Alojamiento alojamiento = cargador.alojamiento(new AlojamientoId(comando.alojamientoId()));
        Estancia estancia = new Estancia(comando.entrada(), comando.salida());

        return apartamentos.buscarActivos(alojamiento.id()).stream()
                .filter(a -> disponibilidad.verificarDisponibilidad(a, estancia, comando.ocupantes(),
                        reservas.buscarActivasPorApartamento(a.codigo()), alojamiento.parametros()).disponible())
                .map(a -> new ApartamentoDisponibleResult(a.codigo().valor(), a.nombre(), a.capacidad().valor(),
                        a.dormitorios().cantidad()))
                .toList();
    }
}
