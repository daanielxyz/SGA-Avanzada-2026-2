package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.BloqueoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.BloqueoOperativoDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador bloquea un apartamento en un rango de fechas (CU-32). Que solo el administrador
 * bloquee (BLO-04) lo controla la seguridad (B3 · DEC-22).
 */
@Service
public class RegistrarBloqueo {

    private final ApartamentoRepository apartamentos;
    private final ReservaRepository reservas;
    private final BloqueoOperativoDomainService bloqueos;
    private final GeneradorCodigos codigos;

    public RegistrarBloqueo(ApartamentoRepository apartamentos, ReservaRepository reservas,
                            BloqueoOperativoDomainService bloqueos, GeneradorCodigos codigos) {
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.bloqueos = bloqueos;
        this.codigos = codigos;
    }

    /**
     * @throws RecursoNoEncontradoException si el apartamento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el rango o el
     *                                      motivo son inválidos (BLO-02) o una reserva activa ocupa esas noches
     *                                      (BLO-01)
     */
    @Transactional
    public ApartamentoResult ejecutar(RegistrarBloqueoCommand comando) {
        ApartamentoId id = new ApartamentoId(comando.codigo());
        Apartamento apartamento = apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));

        bloqueos.registrarBloqueo(apartamento, new BloqueoId(codigos.siguiente(SerieCodigo.BLOQUEO)),
                comando.inicio(), comando.fin(), comando.motivo(), reservas.buscarActivasPorApartamento(id));

        apartamentos.guardar(apartamento);
        return ApartamentoResult.de(apartamento);
    }
}
