package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;

import java.util.List;

/**
 * Servicio de dominio sin estado: decide si un apartamento puede ponerse a la venta. Cruza Apartamento,
 * CalendarioTemporadas y Tarifa, que recibe por parámetro. El resultado se pasa a {@code Apartamento.activar}.
 */
public class ActivadorApartamentoService {

    /**
     * Un apartamento puede activarse si tiene tarifa en todas las temporadas activas, incluida la base
     * (TAR-03 · APA-11), y el calendario tiene el mínimo configurable de temporadas específicas (TEM-04 · DEC-28).
     *
     * @param minimoTemporadas {@code ParametrosAlojamiento.minimoTemporadas}
     * @return {@code true} si se cumplen ambas condiciones
     */
    public boolean puedeActivarse(Apartamento apartamento, CalendarioTemporadas calendario, List<Tarifa> tarifas,
                                  int minimoTemporadas) {
        boolean tarifasCompletas = calendario.temporadasActivas().stream()
                .allMatch(temporada -> tarifas.stream()
                        .anyMatch(t -> t.aplicaA(apartamento.codigo(), temporada.id())));
        return tarifasCompletas && calendario.cantidadTemporadasEspecificas() >= minimoTemporadas;
    }

    /**
     * Pone el apartamento a la venta: exige la variedad de capacidades de la Ficha entre los activos, contando este
     * (CAP-05 · DEC-51), y luego tarifas completas y el mínimo de temporadas ({@link #puedeActivarse}).
     *
     * @param activos    apartamentos activos del alojamiento ({@code ApartamentoRepository.buscarActivos})
     * @param parametros del alojamiento: {@code minimoTemporadas} y {@code minimoCapacidadesDistintas}
     * @throws ReglaDominioException si faltan capacidades distintas (CAP-05), ya está activo, no tiene imágenes
     *                               (IMG-01) o le faltan tarifas o temporadas (TAR-03 · TEM-04)
     */
    public void activar(Apartamento apartamento, CalendarioTemporadas calendario, List<Tarifa> tarifas,
                        List<Apartamento> activos, ParametrosAlojamiento parametros) {
        Capacidad.exigirVariedad(
                CambioCapacidadDomainService.capacidadesCon(apartamento, apartamento.capacidad(), activos),
                parametros.minimoCapacidadesDistintas());
        apartamento.activar(puedeActivarse(apartamento, calendario, tarifas, parametros.minimoTemporadas()));
    }

    /**
     * Una temporada nueva nace con la tarifa de cada apartamento activo o no se crea (TAR-03). Cada apartamento
     * tiene una sola tarifa en la temporada (TAR-01).
     *
     * @param activos apartamentos activos del alojamiento
     * @param tarifas tarifas que llegan con la temporada nueva
     * @throws ReglaDominioException si algún apartamento activo queda sin tarifa en la temporada o un apartamento
     *                               trae dos
     */
    public void exigirTarifasDeTemporada(List<Apartamento> activos, TemporadaId temporada, List<Tarifa> tarifas) {
        if (tarifas.stream().map(Tarifa::apartamentoId).distinct().count() != tarifas.size()) {
            throw new ReglaDominioException("Un apartamento no puede tener dos tarifas en la misma temporada");
        }
        activos.stream()
                .filter(a -> tarifas.stream().noneMatch(t -> t.aplicaA(a.codigo(), temporada)))
                .findFirst()
                .ifPresent(a -> {
                    throw new ReglaDominioException("La temporada nueva necesita la tarifa del apartamento activo "
                            + a.codigo().valor());
                });
    }
}
