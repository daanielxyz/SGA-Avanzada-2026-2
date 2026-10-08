package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.util.List;
import java.util.stream.Stream;

/**
 * Servicio de dominio sin estado: cambia la capacidad de un apartamento. Cruza Apartamento (y los demás activos) y
 * Reservas, que recibe por parámetro.
 */
public class CambioCapacidadDomainService {

    /**
     * Reemplaza la capacidad (CAP-04) sin tocar las reservas ya creadas, aunque queden por encima: las devuelve para
     * advertir al administrador (APA-15). Si el apartamento está en venta, los activos deben conservar la variedad de
     * capacidades de la Ficha (CAP-05 · DEC-51).
     *
     * @param activos                    apartamentos activos del alojamiento ({@code buscarActivos})
     * @param reservas                   reservas activas del apartamento ({@code buscarActivasPorApartamento})
     * @param minimoCapacidadesDistintas {@code ParametrosAlojamiento.minimoCapacidadesDistintas}
     * @return reservas activas cuyo grupo ya no cabe con la nueva capacidad
     * @throws ReglaDominioException si el apartamento está activo y el cambio deja a los activos sin la variedad de
     *                               capacidades exigida (CAP-05)
     */
    public List<Reserva> cambiarCapacidad(Apartamento apartamento, Capacidad nueva, List<Apartamento> activos,
                                          List<Reserva> reservas, int minimoCapacidadesDistintas) {
        if (apartamento.activo()) {
            Capacidad.exigirVariedad(capacidadesCon(apartamento, nueva, activos), minimoCapacidadesDistintas);
        }
        apartamento.cambiarCapacidad(nueva);
        return reservas.stream()
                .filter(r -> r.apartamentoId().equals(apartamento.codigo()) && r.estaActiva()
                        && !nueva.admite(r.ocupantes().size()))
                .toList();
    }

    /**
     * Capacidades de los activos como quedarían si {@code apartamento} está en venta con {@code capacidad}.
     */
    static List<Capacidad> capacidadesCon(Apartamento apartamento, Capacidad capacidad, List<Apartamento> activos) {
        return Stream.concat(
                        activos.stream().filter(a -> !a.equals(apartamento)).map(Apartamento::capacidad),
                        Stream.of(capacidad))
                .toList();
    }
}
