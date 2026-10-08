package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CambioCapacidadDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: el administrador cambia la capacidad de un apartamento (CU-31 · CAP-04). Las reservas ya creadas no
 * cambian; se devuelven las que quedan por encima para advertirlo (APA-15).
 */
@Service
public class CambiarCapacidad {

    private final ApartamentoRepository apartamentos;
    private final AlojamientoRepository alojamientos;
    private final ReservaRepository reservas;
    private final CambioCapacidadDomainService cambioCapacidad;

    public CambiarCapacidad(ApartamentoRepository apartamentos, AlojamientoRepository alojamientos,
                            ReservaRepository reservas, CambioCapacidadDomainService cambioCapacidad) {
        this.apartamentos = apartamentos;
        this.alojamientos = alojamientos;
        this.reservas = reservas;
        this.cambioCapacidad = cambioCapacidad;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el apartamento o su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la capacidad es
     *         menor que 1 (CAP-01) o, estando a la venta, los activos quedarían sin la variedad exigida (CAP-05)
     */
    @Transactional
    public CambioCapacidadResult ejecutar(CambiarCapacidadCommand comando) {
        ApartamentoId id = new ApartamentoId(comando.codigo());
        Apartamento apartamento = apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));
        Alojamiento alojamiento = alojamientos.buscarPorId(apartamento.alojamientoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento",
                        apartamento.alojamientoId().valor()));

        List<Reserva> afectadas = cambioCapacidad.cambiarCapacidad(apartamento, new Capacidad(comando.capacidad()),
                apartamentos.buscarActivos(alojamiento.id()), reservas.buscarActivasPorApartamento(id),
                alojamiento.parametros().minimoCapacidadesDistintas());

        apartamentos.guardar(apartamento);
        return new CambioCapacidadResult(ApartamentoResult.de(apartamento),
                afectadas.stream().map(r -> r.codigo().valor()).toList());
    }
}
