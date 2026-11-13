package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Servicio de dominio sin estado: ordena los apartamentos por preparar según la ocupación más cercana que tienen
 * (CU-41 · DEC-111), para que el personal de servicio empiece por el que se necesita primero sin esperar a que
 * recepción se lo indique. Cruza Apartamento y Reserva, que recibe por parámetro; no cambia ninguno.
 *
 * <p>El orden es lo único que se entrega: el personal de servicio no ve las reservas, así que ni la fecha ni la hora
 * de la próxima entrada salen de aquí.
 */
public class PreparacionDomainService {

    /** La entrada más temprana primero; el mismo día, primero la que tiene hora estimada, de la más temprana. */
    private static final Comparator<Reserva> POR_LLEGADA = Comparator
            .comparing((Reserva r) -> r.estancia().entrada())
            .thenComparing(Reserva::horaEstimadaLlegada, Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * CU-41 · DEC-111: ordena los apartamentos por la próxima llegada esperada de cada uno
     * ({@link Reserva#llegadaEsperada}); los que no esperan a nadie van al final. Si empatan, por código.
     *
     * @param pendientes los apartamentos por preparar
     * @param reservas   las reservas activas de esos apartamentos; las de otros apartamentos no cambian nada
     * @param ahora      fecha y hora actuales en Colombia, inyectadas
     * @param horaLimite {@code ParametrosAlojamiento.horaLimiteNoShow}
     * @return los mismos apartamentos, en el orden en que conviene prepararlos
     */
    public List<Apartamento> ordenarPorProximaLlegada(List<Apartamento> pendientes, List<Reserva> reservas,
                                                      LocalDateTime ahora, LocalTime horaLimite) {
        Objects.requireNonNull(pendientes, "pendientes");
        Objects.requireNonNull(reservas, "reservas");
        Objects.requireNonNull(ahora, "ahora");
        Objects.requireNonNull(horaLimite, "horaLimite");

        Map<ApartamentoId, Reserva> proximaLlegada = reservas.stream()
                .filter(r -> r.llegadaEsperada(ahora, horaLimite))
                .collect(Collectors.toMap(Reserva::apartamentoId, r -> r, BinaryOperator.minBy(POR_LLEGADA)));

        return pendientes.stream()
                .sorted(Comparator.comparing((Apartamento a) -> proximaLlegada.get(a.codigo()),
                                Comparator.nullsLast(POR_LLEGADA))
                        .thenComparing(a -> a.codigo().valor()))
                .toList();
    }
}
