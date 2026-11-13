package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.PreparacionDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: el personal de servicio consulta qué apartamentos tiene por preparar, de 10 en 10 (CU-41 · DEC-18),
 * empezando por el que tiene la ocupación más cercana (DEC-111). Solo entrega el orden: ningún dato de las reservas.
 */
@Service
public class ListarPendientesPreparacionUseCase {

    private final ApartamentoRepository apartamentos;
    private final ReservaRepository reservas;
    private final AlojamientoRepository alojamientos;
    private final PreparacionDomainService preparacion;
    private final Clock reloj;

    public ListarPendientesPreparacionUseCase(ApartamentoRepository apartamentos, ReservaRepository reservas,
                                              AlojamientoRepository alojamientos,
                                              PreparacionDomainService preparacion, Clock reloj) {
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.alojamientos = alojamientos;
        this.preparacion = preparacion;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el alojamiento
     */
    @Transactional(readOnly = true)
    public Pagina<ApartamentoResult> ejecutar(String alojamientoId, int numeroPagina) {
        AlojamientoId id = new AlojamientoId(alojamientoId);
        Alojamiento alojamiento = alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", alojamientoId));
        List<Apartamento> pendientes = apartamentos.buscarActivos(id).stream()
                .filter(Apartamento::pendienteDePreparacion)
                .toList();
        List<Reserva> activas = pendientes.stream()
                .flatMap(a -> reservas.buscarActivasPorApartamento(a.codigo()).stream())
                .toList();

        List<Apartamento> enOrden = preparacion.ordenarPorProximaLlegada(pendientes, activas,
                LocalDateTime.now(reloj), alojamiento.parametros().horaLimiteNoShow());

        return Pagina.de(enOrden.stream().map(ApartamentoResult::de).toList(), numeroPagina);
    }
}
