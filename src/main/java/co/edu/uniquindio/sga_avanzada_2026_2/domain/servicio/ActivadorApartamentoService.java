package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;

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
}
