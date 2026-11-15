package co.edu.uniquindio.sga_avanzada_2026_2.domain.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.MotivoViaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Servicio de dominio sin estado: arma la Tarjeta de Registro de Alojamiento de una reserva (Resolución 409 de 2022,
 * DEC-130). Recibe los datos del establecimiento por parámetro; no usa repositorios.
 */
public class ReporteTraDomainService {

    /**
     * Una reserva se reporta cuando el grupo ya llegó (EN_CURSO o FINALIZADA) y a nadie le falta un dato del TRA
     * (BL-07 · DEC-96 · DEC-130).
     */
    public boolean listaParaEnviar(Reserva reserva) {
        return (reserva.estado() == EstadoReserva.EN_CURSO || reserva.estado() == EstadoReserva.FINALIZADA)
                && reserva.ocupantes().stream().allMatch(o -> o.datosTraFaltantes().isEmpty());
    }

    /**
     * @throws ReglaDominioException si el grupo no ha llegado o a alguien le falta un dato del TRA
     */
    // DEC-130
    public void exigirListaParaEnviar(Reserva reserva) {
        if (!listaParaEnviar(reserva)) {
            throw new ReglaDominioException("El TRA de la reserva " + reserva.codigo().valor()
                    + " se envía cuando el grupo llegó y están todos sus datos");
        }
    }

    /**
     * @param tipoAcomodacion configuración del alojamiento («Apartamento»)
     * @param rnt             el Registro Nacional de Turismo del alojamiento
     * @throws ReglaDominioException si la reserva no está lista para enviarse o falta el RNT
     */
    // DEC-130
    public ReporteTra armar(Reserva reserva, String nombreEstablecimiento, String rnt, String tipoAcomodacion) {
        exigirListaParaEnviar(reserva);
        if (rnt == null || rnt.isBlank()) {
            throw new ReglaDominioException("Falta el RNT del alojamiento: escríbelo en «Alojamiento»");
        }
        Ocupante principal = principalDe(reserva);
        List<HuespedTra> acompanantes = reserva.ocupantes().stream()
                .filter(o -> !o.id().equals(principal.id()))
                .map(ReporteTraDomainService::huesped)
                .toList();
        return new ReporteTra(reserva.codigo(), huesped(principal), acompanantes,
                texto(principal.viaje().motivoViaje()), reserva.estancia().entrada(), reserva.estancia().salida(),
                reserva.apartamentoId().valor(), tipoAcomodacion, reserva.valorTotal(), nombreEstablecimiento,
                rnt.trim());
    }

    /** El huésped principal del grupo (DEC-97); en las reservas de antes, el primero. */
    private static Ocupante principalDe(Reserva reserva) {
        return reserva.ocupantes().stream().filter(Ocupante::principal).findFirst()
                .orElseGet(() -> reserva.ocupantes().getFirst());
    }

    private static HuespedTra huesped(Ocupante ocupante) {
        Documento documento = Objects.requireNonNull(ocupante.documento(), "documento");
        String apellidos = Stream.of(ocupante.primerApellido(), ocupante.segundoApellido())
                .filter(Objects::nonNull).collect(Collectors.joining(" "));
        return new HuespedTra(ocupante.id(), tipo(documento.tipo()), documento.numero(), ocupante.nombres(),
                apellidos, ocupante.viaje().ciudadResidencia(), ocupante.viaje().ciudadProcedencia());
    }

    // TODO(equipo): confirmar con el Ministerio los textos de cada tipo; el manual solo muestra «C.C» y «Pasaporte»
    static String tipo(TipoDocumento tipo) {
        return switch (tipo) {
            case CC -> "C.C";
            case CE -> "C.E";
            case TI -> "T.I";
            case PASAPORTE -> "Pasaporte";
            case RC -> "R.C";
        };
    }

    /** El motivo en palabras: el Ministerio lo recibe como texto libre. */
    static String texto(MotivoViaje motivo) {
        return switch (motivo) {
            case TURISMO_VACACIONES -> "Turismo y vacaciones";
            case EVENTOS -> "Eventos";
            case ESTUDIOS -> "Estudios";
            case TRATAMIENTO_MEDICO -> "Tratamiento médico";
            case ASUNTOS_JURIDICOS -> "Asuntos jurídicos";
            case NEGOCIOS -> "Negocios";
            case TRANSITO -> "Tránsito";
            case COMPRAS -> "Compras";
            case RELIGION -> "Religión";
            case VISITA_FAMILIARES_AMIGOS -> "Visita a familiares y amigos";
        };
    }
}
