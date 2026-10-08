package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ActivadorApartamentoService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Caso de uso: el administrador pone un apartamento a la venta (CU-31 · APA-11). Solo se guarda el apartamento;
 * calendario, tarifas y demás activos se leen para decidir.
 */
@Service
public class ActivarApartamento {

    private final ApartamentoRepository apartamentos;
    private final AlojamientoRepository alojamientos;
    private final CalendarioTemporadasRepository calendarios;
    private final TarifaRepository tarifas;
    private final ActivadorApartamentoService activador;
    private final Clock reloj;

    public ActivarApartamento(ApartamentoRepository apartamentos, AlojamientoRepository alojamientos,
                              CalendarioTemporadasRepository calendarios, TarifaRepository tarifas,
                              ActivadorApartamentoService activador, Clock reloj) {
        this.apartamentos = apartamentos;
        this.alojamientos = alojamientos;
        this.calendarios = calendarios;
        this.tarifas = tarifas;
        this.activador = activador;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el apartamento, su alojamiento o el calendario
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si ya está activo, no
     *         tiene imágenes (IMG-01), le faltan tarifas vigentes o temporadas (TAR-03 · TEM-04) o los activos
     *         quedarían sin la variedad de capacidades exigida (CAP-05)
     */
    @Transactional
    public ApartamentoResult ejecutar(String codigo) {
        ApartamentoId id = new ApartamentoId(codigo);
        Apartamento apartamento = apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));
        Alojamiento alojamiento = alojamientos.buscarPorId(apartamento.alojamientoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento",
                        apartamento.alojamientoId().valor()));
        CalendarioTemporadas calendario = calendarios.buscarPorAlojamiento(alojamiento.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("el calendario de temporadas del alojamiento",
                        alojamiento.id().valor()));
        LocalDate hoy = LocalDate.now(reloj);

        activador.activar(apartamento, calendario, tarifas.buscarVigentesPorApartamento(id, hoy),
                apartamentos.buscarActivos(alojamiento.id()), alojamiento.parametros());

        apartamentos.guardar(apartamento);
        return ApartamentoResult.de(apartamento);
    }
}
