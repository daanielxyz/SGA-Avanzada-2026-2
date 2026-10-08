package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
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

/**
 * Caso de uso: el administrador define el valor de un apartamento en una temporada (CU-34). La primera vez crea la
 * versión 1; después crea la versión siguiente y conserva la anterior (TAR-05). Rige desde hoy: las reservas ya
 * creadas no cambian (TAR-06 · DEC-54).
 */
@Service
public class DefinirTarifa {

    private final TarifaRepository tarifas;
    private final ApartamentoRepository apartamentos;
    private final CalendarioTemporadasRepository calendarios;
    private final GeneradorCodigos codigos;
    private final Clock reloj;

    public DefinirTarifa(TarifaRepository tarifas, ApartamentoRepository apartamentos,
                         CalendarioTemporadasRepository calendarios, GeneradorCodigos codigos, Clock reloj) {
        this.tarifas = tarifas;
        this.apartamentos = apartamentos;
        this.calendarios = calendarios;
        this.codigos = codigos;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el apartamento o el calendario de su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la temporada no
     *         existe o está inactiva (TAR-01), o el valor no es positivo (TAR-02)
     */
    @Transactional
    public TarifaResult ejecutar(DefinirTarifaCommand comando) {
        ApartamentoId apartamentoId = new ApartamentoId(comando.apartamento());
        Apartamento apartamento = apartamentos.buscarPorCodigo(apartamentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", apartamentoId.valor()));
        CalendarioTemporadas calendario = calendarios.buscarPorAlojamiento(apartamento.alojamientoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("el calendario de temporadas del alojamiento",
                        apartamento.alojamientoId().valor()));
        TemporadaId temporadaId = new TemporadaId(comando.temporadaId());
        calendario.exigirTemporadaActiva(temporadaId);
        TarifaId id = new TarifaId(codigos.siguiente(SerieCodigo.TARIFA));
        Dinero valor = new Dinero(comando.valorPorOcupante());
        LocalDate hoy = LocalDate.now(reloj);

        Tarifa nueva = tarifas.buscarVigentesPorApartamento(apartamentoId, hoy).stream()
                .filter(t -> t.aplicaA(apartamentoId, temporadaId))
                .findFirst()
                .map(vigente -> vigente.nuevaVersion(id, valor, hoy))
                .orElseGet(() -> Tarifa.crear(id, apartamentoId, temporadaId, valor, hoy));

        tarifas.guardar(nueva);
        return TarifaResult.de(nueva);
    }
}
