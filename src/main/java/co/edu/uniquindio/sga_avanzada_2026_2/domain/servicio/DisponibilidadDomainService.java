package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de dominio sin estado: decide si un apartamento está disponible para una estancia. Cruza Apartamento y
 * Reservas, que recibe por parámetro (DISP-02).
 */
public class DisponibilidadDomainService {

    /**
     * Disponible si en todas las noches: el apartamento está activo y su capacidad alcanza (DISP-02 · RN-02), no
     * tiene bloqueo vigente (RN-07 · BLO-03), no hay reserva activa solapada de ningún canal (RN-01 · CORI-02) y se
     * respeta el tiempo de preparación con la reserva anterior y la siguiente (RN-20 · TPRE-02 · TPRE-03). Las
     * horas solo intervienen a través del tiempo de preparación (DISP-04).
     *
     * @param reservas   reservas del apartamento ({@code ReservaRepository.buscarActivasPorApartamento}); las de
     *                   otro apartamento o inactivas se ignoran
     * @param parametros horas de entrada y salida y tiempo de preparación del alojamiento
     * @return el resultado con el primer motivo que lo impide; nunca lanza por falta de disponibilidad
     */
    public Disponibilidad verificarDisponibilidad(Apartamento apartamento, Estancia estancia, int ocupantes,
                                                  List<Reserva> reservas, ParametrosAlojamiento parametros) {
        List<Reserva> activas = reservas.stream()
                .filter(r -> r.apartamentoId().equals(apartamento.codigo()) && r.estaActiva())
                .toList();
        return motivo(apartamento, estancia, ocupantes, activas, parametros)
                .map(m -> Disponibilidad.ocupada(apartamento.codigo(), estancia, m))
                .orElseGet(() -> Disponibilidad.libre(apartamento.codigo(), estancia));
    }

    /**
     * Igual que {@link #verificarDisponibilidad}, sin contar la reserva que se está modificando: sus propias noches
     * no le quitan disponibilidad (RN-14).
     */
    public Disponibilidad verificarParaModificar(Reserva reserva, Apartamento apartamento, Estancia estancia,
                                                 int ocupantes, List<Reserva> reservas,
                                                 ParametrosAlojamiento parametros) {
        List<Reserva> otras = reservas.stream().filter(r -> !r.equals(reserva)).toList();
        return verificarDisponibilidad(apartamento, estancia, ocupantes, otras, parametros);
    }

    private static Optional<String> motivo(Apartamento apartamento, Estancia estancia, int ocupantes,
                                           List<Reserva> activas, ParametrosAlojamiento parametros) {
        String codigo = apartamento.codigo().valor();
        if (!apartamento.activo()) {
            return Optional.of("El apartamento " + codigo + " no está a la venta");
        }
        if (!apartamento.admite(ocupantes)) {
            return Optional.of("El apartamento " + codigo + " admite máximo " + apartamento.capacidad().valor()
                    + " ocupantes");
        }
        Optional<Noche> bloqueada = estancia.noches().stream().filter(apartamento::tieneBloqueoEn).findFirst();
        if (bloqueada.isPresent()) {
            return Optional.of("El apartamento " + codigo + " está bloqueado la noche " + bloqueada.get().fecha());
        }
        Optional<Reserva> solapada = activas.stream().filter(r -> r.retieneNochesDe(estancia)).findFirst();
        if (solapada.isPresent()) {
            return Optional.of("El apartamento " + codigo + " ya está reservado en esas fechas ("
                    + solapada.get().codigo().valor() + ")");
        }
        if (!hayRecambioElMismoDia(parametros)) {
            Optional<Reserva> contigua = activas.stream()
                    .filter(r -> r.estancia().salida().equals(estancia.entrada())
                            || r.estancia().entrada().equals(estancia.salida()))
                    .findFirst();
            if (contigua.isPresent()) {
                return Optional.of("No queda el tiempo de preparación entre la salida y la entrada junto a la reserva "
                        + contigua.get().codigo().valor());
            }
        }
        return Optional.empty();
    }

    // TPRE-02: el tiempo de preparación debe caber entre la hora de salida y la hora de entrada del mismo día
    private static boolean hayRecambioElMismoDia(ParametrosAlojamiento parametros) {
        Duration ventana = Duration.between(parametros.horaSalida(), parametros.horaEntrada());
        return parametros.tiempoPreparacion().compareTo(ventana) <= 0;
    }
}
