package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.BajaApartamentoDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador retira un apartamento de la venta (eliminación lógica, CU-31 · APA-16).
 */
@Service
public class RetirarApartamento {

    private final ApartamentoRepository apartamentos;
    private final ReservaRepository reservas;
    private final BajaApartamentoDomainService baja;

    public RetirarApartamento(ApartamentoRepository apartamentos, ReservaRepository reservas,
                              BajaApartamentoDomainService baja) {
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.baja = baja;
    }

    /**
     * @throws RecursoNoEncontradoException si el apartamento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si ya estaba retirado
     *                                      o tiene reservas activas o futuras (APA-16)
     */
    @Transactional
    public ApartamentoResult ejecutar(String codigo) {
        ApartamentoId id = new ApartamentoId(codigo);
        Apartamento apartamento = apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));

        baja.retirarDeVenta(apartamento, reservas.buscarActivasPorApartamento(id));

        apartamentos.guardar(apartamento);
        return ApartamentoResult.de(apartamento);
    }
}
