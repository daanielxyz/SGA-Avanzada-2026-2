package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Servicio de dominio sin estado: integra una reserva enviada por un canal externo (CU-12). Cruza Canal, Reserva y
 * ConflictoCanal, que recibe por parámetro; no usa repositorios (DEC-48).
 */
public class SincronizacionCanalDomainService {

    /**
     * Decide qué pasa con una reserva externa, en este orden:
     * <ol>
     *   <li>el canal debe ser EXTERNO y estar activo (CAN-05);</li>
     *   <li>si ese canal ya envió ese identificador, devuelve la reserva existente y no crea otra (RN-19 ·
     *       CAN-04);</li>
     *   <li>si el apartamento no está disponible, por la razón que sea, registra un {@link ConflictoCanal} con el
     *       motivo y no toca la reserva vigente (RN-18 · CONF-01 · CONF-02 · DEC-48);</li>
     *   <li>si no, crea la reserva, que nace PENDIENTE (D-08) con su canal e identificador (CORI-03).</li>
     * </ol>
     *
     * @param yaRecibida    {@code ReservaRepository.buscarPorCanalEIdExterno}; la restricción única en la BD cubre los
     *                      mensajes simultáneos (DEC-17)
     * @param disponibilidad de {@code DisponibilidadDomainService.verificarDisponibilidad} para lo que pidió el canal
     * @param crearReserva  arma la reserva con {@code Reserva.crear}; solo se invoca si hay disponibilidad
     * @param conflictoId   id para el conflicto, por si hay que registrarlo
     * @param ahora         fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si el canal no es externo o está inactivo, la reserva existente no corresponde a
     *                               ese canal e identificador, o {@code Reserva.crear} rechaza los datos
     */
    public IntegracionExterna integrarReservaExterna(Canal canal, String idExterno, Optional<Reserva> yaRecibida,
                                                     Disponibilidad disponibilidad, Supplier<Reserva> crearReserva,
                                                     ConflictoId conflictoId, LocalDateTime ahora) {
        canal.exigirReservasExternas();
        if (yaRecibida.isPresent()) {
            validarOrigen(yaRecibida.get(), canal, idExterno);
            return new IntegracionExterna(ResultadoEvento.DUPLICADO, yaRecibida.get(), null);
        }
        if (!disponibilidad.disponible()) {
            ConflictoCanal conflicto = ConflictoCanal.registrar(conflictoId, canal.id(), idExterno,
                    disponibilidad.apartamentoId(), disponibilidad.estancia(), ahora, disponibilidad.motivo());
            return new IntegracionExterna(ResultadoEvento.CONFLICTO, null, conflicto);
        }
        Reserva nueva = crearReserva.get();
        validarOrigen(nueva, canal, idExterno);
        return new IntegracionExterna(ResultadoEvento.EXITOSO, nueva, null);
    }

    // CORI-03 · RN-19
    private static void validarOrigen(Reserva reserva, Canal canal, String idExterno) {
        if (reserva.canalOrigen() != CanalOrigen.EXTERNO || !canal.id().equals(reserva.canalId())
                || !reserva.idExterno().equals(idExterno)) {
            throw new ReglaDominioException("La reserva " + reserva.codigo().valor() + " no corresponde al canal "
                    + canal.id().valor() + " con identificador " + idExterno);
        }
    }
}
