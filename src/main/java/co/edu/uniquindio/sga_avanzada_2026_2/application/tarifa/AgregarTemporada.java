package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.CalendarioResult.TemporadaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ActivadorApartamentoService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso: el administrador agrega una temporada específica (CU-33). Nace junto con la tarifa de cada
 * apartamento activo o no se crea (TAR-03): guarda el calendario y las tarifas en la misma transacción (DEC-54).
 */
@Service
public class AgregarTemporada {

    private final CalendarioTemporadasRepository calendarios;
    private final TarifaRepository tarifas;
    private final ApartamentoRepository apartamentos;
    private final ActivadorApartamentoService activador;
    private final GeneradorCodigos codigos;
    private final Clock reloj;

    public AgregarTemporada(CalendarioTemporadasRepository calendarios, TarifaRepository tarifas,
                            ApartamentoRepository apartamentos, ActivadorApartamentoService activador,
                            GeneradorCodigos codigos, Clock reloj) {
        this.calendarios = calendarios;
        this.tarifas = tarifas;
        this.apartamentos = apartamentos;
        this.activador = activador;
        this.codigos = codigos;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el calendario del alojamiento o un apartamento de las
     *                                      tarifas
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el rango es
     *         inválido o se solapa (TEM-01 · TEM-02), falta la tarifa de un apartamento activo o se repite
     *         (TAR-03 · TAR-01), o un valor no es positivo (TAR-02)
     */
    @Transactional
    public TemporadaCreadaResult ejecutar(AgregarTemporadaCommand comando) {
        AlojamientoId alojamientoId = new AlojamientoId(comando.alojamientoId());
        CalendarioTemporadas calendario = calendarios.buscarPorAlojamiento(alojamientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el calendario de temporadas del alojamiento",
                        alojamientoId.valor()));
        TemporadaId temporadaId = new TemporadaId(codigos.siguiente(SerieCodigo.TEMPORADA));
        LocalDate hoy = LocalDate.now(reloj);

        calendario.agregarTemporada(temporadaId, comando.nombre(), comando.fechaInicio(), comando.fechaFin(),
                comando.estanciaMinimaNoches());
        List<Tarifa> nuevas = comando.tarifas().stream()
                .map(t -> Tarifa.crear(new TarifaId(codigos.siguiente(SerieCodigo.TARIFA)),
                        apartamentoExistente(t.apartamento()), temporadaId, new Dinero(t.valorPorOcupante()), hoy))
                .toList();
        activador.exigirTarifasDeTemporada(apartamentos.buscarActivos(alojamientoId), temporadaId, nuevas);

        calendarios.guardar(calendario);
        nuevas.forEach(tarifas::guardar);
        TemporadaResult temporada = calendario.temporadas().stream()
                .filter(t -> t.id().equals(temporadaId))
                .map(TemporadaResult::de)
                .findFirst()
                .orElseThrow();
        return new TemporadaCreadaResult(temporada, nuevas.stream().map(TarifaResult::de).toList());
    }

    private ApartamentoId apartamentoExistente(String codigo) {
        ApartamentoId id = new ApartamentoId(codigo);
        return apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()))
                .codigo();
    }
}
