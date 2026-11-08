package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MotivoRechazo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.util.List;

/**
 * RP-02: el parqueadero es de todo el alojamiento, así que ninguna reserva sola puede saber si su vehículo cabe; hay
 * que sumar, noche por noche, los de todas las reservas activas. Recibe los datos por parámetro.
 */
public class CupoParqueaderoDomainService {

    /**
     * @param activas  reservas activas del alojamiento que declararon vehículos
     * @param excluida la reserva que se está modificando, para no contarla dos veces; {@code null} al crear
     * @param cupo     {@code ParametrosAlojamiento.cupoParqueadero}
     * @throws ReglaDominioException si en alguna noche de la estancia los vehículos superan el cupo
     */
    public void exigirCupo(Estancia estancia, int vehiculos, List<Reserva> activas, Reserva excluida, int cupo) {
        if (vehiculos == 0) {
            return;
        }
        estancia.noches().forEach(noche -> {
            int ocupados = activas.stream()
                    .filter(r -> !r.equals(excluida) && r.estancia().noches().contains(noche))
                    .mapToInt(Reserva::vehiculos)
                    .sum();
            if (ocupados + vehiculos > cupo) {
                throw new ReglaDominioException(MotivoRechazo.SIN_CUPO_PARQUEADERO,
                        "El parqueadero no tiene cupo para " + vehiculos + " vehículo(s) la "
                        + "noche del " + noche.fecha() + ": admite " + cupo + " y ya hay " + ocupados);
            }
        });
    }
}
