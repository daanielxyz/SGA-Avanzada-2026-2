package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

/**
 * Caso de uso: calendario de ocupación por apartamento y rango de fechas, de 10 en 10 (CU-24 · sección 7.5 · DEC-18).
 */
@Service
public class ConsultarOcupacionUseCase {

    private final ApartamentoRepository apartamentos;
    private final ReservaRepository reservas;

    public ConsultarOcupacionUseCase(ApartamentoRepository apartamentos, ReservaRepository reservas) {
        this.apartamentos = apartamentos;
        this.reservas = reservas;
    }

    /**
     * @param hasta       exclusiva, como la salida de una estancia (EST-01)
     * @param apartamento opcional: solo ese apartamento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el rango es inválido (EST-02)
     */
    @Transactional(readOnly = true)
    public Pagina<OcupacionResult> ejecutar(String alojamientoId, LocalDate desde, LocalDate hasta, String apartamento,
                                            int numeroPagina) {
        Estancia rango = new Estancia(desde, hasta);
        return Pagina.de(apartamentos.buscarActivos(new AlojamientoId(alojamientoId)).stream()
                .filter(a -> Optional.ofNullable(apartamento).map(a.codigo().valor()::equals).orElse(true))
                .sorted(Comparator.comparing(a -> a.codigo().valor()))
                .map(a -> fila(a, rango))
                .toList(), numeroPagina);
    }

    private OcupacionResult fila(Apartamento a, Estancia rango) {
        return new OcupacionResult(a.codigo().valor(), a.nombre(), a.estadoOperativo().name(),
                reservas.buscarActivasPorApartamento(a.codigo()).stream()
                        .filter(r -> r.estancia().seSolapaCon(rango))
                        .sorted(Comparator.comparing(r -> r.estancia().entrada()))
                        .map(r -> new OcupacionResult.ReservaOcupada(r.codigo().valor(), r.estancia().entrada(),
                                r.estancia().salida(), r.estado().name(), r.canalOrigen().name(),
                                r.titularId().valor()))
                        .toList(),
                a.bloqueosVigentesEn(rango).stream()
                        .map(b -> new OcupacionResult.BloqueoOcupado(b.id().valor(), b.fechaInicio(), b.fechaFin(),
                                b.motivo()))
                        .toList());
    }
}
