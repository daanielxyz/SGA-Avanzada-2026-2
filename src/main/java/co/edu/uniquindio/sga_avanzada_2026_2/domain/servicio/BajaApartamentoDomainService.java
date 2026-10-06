package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.util.List;

/**
 * Servicio de dominio sin estado: retira un apartamento de la venta. Cruza Apartamento y Reservas, que recibe por
 * parámetro.
 */
public class BajaApartamentoDomainService {

    /**
     * Retira el apartamento solo si no tiene reservas activas ni futuras; las futuras son PENDIENTE o CONFIRMADA,
     * así que basta con las activas (APA-16 · EDO-03).
     *
     * @param reservas reservas del apartamento ({@code ReservaRepository.buscarActivasPorApartamento})
     * @throws ReglaDominioException si ya estaba retirado o tiene reservas activas o futuras (APA-16)
     */
    public void retirarDeVenta(Apartamento apartamento, List<Reserva> reservas) {
        boolean hayReservasActivas = reservas.stream()
                .anyMatch(r -> r.apartamentoId().equals(apartamento.codigo()) && r.estaActiva());
        apartamento.retirarDeVenta(hayReservasActivas);
    }
}
