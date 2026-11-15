package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Bloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.BloqueoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de dominio sin estado: registra bloqueos sin pisar reservas. Cruza Apartamento y Reservas, que recibe
 * por parámetro.
 */
public class BloqueoOperativoDomainService {

    /**
     * Registra un bloqueo [inicio, fin) solo si ninguna reserva activa del apartamento ocupa esas noches (BLO-01 ·
     * RN-07). Que solo el administrador bloquee (BLO-04) se controla en la aplicación (DEC-22).
     *
     * @param reservas reservas del apartamento ({@code ReservaRepository.buscarActivasPorApartamento})
     * @param hoy      la fecha del alojamiento, del reloj inyectado: un bloqueo no empieza antes (DEC-122)
     * @throws ReglaDominioException si el rango o el motivo son inválidos (BLO-02) o alguna reserva activa ocupa
     *                               esas noches (BLO-01), o empieza antes de hoy (DEC-122)
     */
    public void registrarBloqueo(Apartamento apartamento, BloqueoId id, LocalDate inicio, LocalDate fin,
                                 String motivo, List<Reserva> reservas, LocalDate hoy) {
        new Bloqueo(id, inicio, fin, motivo, true); // valida BLO-02 antes de comparar rangos
        // BLO-02 · DEC-122: bloquear noches que ya pasaron no cambia nada y ensucia el historial
        if (inicio.isBefore(hoy)) {
            throw new ReglaDominioException(
                    "No se puede bloquear desde una fecha que ya pasó: el bloqueo empieza hoy o después");
        }
        Estancia rango = new Estancia(inicio, fin); // misma convención [inicio, fin) (BLO-02 · EST-01)
        reservas.stream()
                .filter(r -> r.apartamentoId().equals(apartamento.codigo()) && r.retieneNochesDe(rango))
                .findFirst()
                .ifPresent(r -> {
                    throw new ReglaDominioException("No se puede bloquear: la reserva " + r.codigo().valor()
                            + " ocupa esas noches");
                });
        apartamento.registrarBloqueo(id, inicio, fin, motivo);
    }
}
