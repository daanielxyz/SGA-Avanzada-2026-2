package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.NocheCalendario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso: qué noches de un apartamento se pueden vender, para el calendario de su detalle (CU-08). No dice quién
 * reservó ni por qué no está libre: es información pública.
 */
@Service
public class ConsultarCalendarioDisponibilidadUseCase {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final DisponibilidadDomainService disponibilidad;

    public ConsultarCalendarioDisponibilidadUseCase(CargadorReserva cargador, ReservaRepository reservas,
                                                    DisponibilidadDomainService disponibilidad) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.disponibilidad = disponibilidad;
    }

    /**
     * @param hasta exclusiva, como la salida de una estancia (EST-01)
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si el apartamento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el rango es inválido o demasiado largo
     */
    @Transactional(readOnly = true)
    public List<NocheCalendario> ejecutar(String codigo, LocalDate desde, LocalDate hasta) {
        Apartamento apartamento = cargador.apartamento(codigo);
        Alojamiento alojamiento = cargador.alojamientoDe(apartamento);
        return disponibilidad.calendario(apartamento, new Estancia(desde, hasta),
                reservas.buscarActivasPorApartamento(apartamento.codigo()), alojamiento.parametros());
    }
}
