package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaConTitularResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * JSON de una reserva (DEC-31): estado, estancia, grupo, valor y política congelados (CU-49).
 *
 * @param salida     exclusiva: esa noche no se ocupa (EST-01)
 * @param valor      el desglose congelado al crear o modificar (RN-22)
 * @param politicaId la versión de la política de cancelación que aceptó el huésped
 * @param vehiculos  carros y motos, que ocupan el mismo cupo (RP-02)
 * @param extras     lo que suman parqueadero y mascotas, aparte de {@code valor} (DEC-89)
 * @param titularNombre solo en el listado de recepción y en el detalle; en las demás respuestas, nulo
 * @param anticipoExigido lo que debe estar pagado para confirmarla, con los extras (RES-15 · DEC-89); solo en el
 *                        detalle, nulo en las demás respuestas
 * @param noShow     quién lo declaró y cuándo (BL-25 · DEC-103); solo en el detalle para el personal; nulo si no
 *                   aplica o si se declaró antes de guardarse este dato
 * @param correccionesDocumento los documentos corregidos, del más antiguo al más reciente (DEC-120); solo en el
 *                              detalle para el personal y al corregir; nulo en las demás respuestas
 */
public record ReservaResponse(String codigo, String apartamento, String titularId, LocalDate entrada,
                              LocalDate salida, String estado, String canalOrigen,
                              List<OcupanteResponse> ocupantes, LocalTime horaEstimadaLlegada,
                              CotizacionResponse valor, String politicaId, LocalDateTime creadaEn,
                              String avisoCuenta, int vehiculos, int mascotas, String titularNombre, int carros,
                              int motos, ExtrasResponse extras, BigDecimal anticipoExigido,
                              DeclaracionNoShowResponse noShow,
                              List<CorreccionDocumentoResponse> correccionesDocumento) {

    /**
     * @param autor       id del usuario
     * @param autorNombre el nombre vigente de esa persona; nulo si ya no existe
     */
    public record DeclaracionNoShowResponse(String autor, String autorNombre, LocalDateTime fechaHora) {
    }

    public record ExtrasResponse(BigDecimal parqueadero, BigDecimal mascotas, BigDecimal total) {
    }

    /**
     * @param ocupante    nombre de la persona cuando se corrigió
     * @param autor       id del usuario que corrigió
     * @param autorNombre el nombre vigente de esa persona; nulo si ya no existe
     * @param titular     si fue el documento del titular (y, si se aloja, el de su ocupante con él)
     */
    public record CorreccionDocumentoResponse(String ocupante, String tipoAnterior, String numeroAnterior,
                                              String tipoNuevo, String numeroNuevo, String motivo, String autor,
                                              String autorNombre, LocalDateTime fechaHora, boolean titular) {
    }

    /**
     * @param nombre            nombres y apellidos juntos
     * @param datosTraFaltantes lo que le falta para el TRA, de {@code DOCUMENTO} a {@code MOTIVO_VIAJE}; vacío si
     *                          está completo. Mientras a alguien le falte algo, la salida responde 409 (BL-07)
     */
    public record OcupanteResponse(String id, String nombre, LocalDate fechaNacimiento, String tipoDocumento,
                                   String numeroDocumento, String nacionalidad, String nombres,
                                   String primerApellido, String segundoApellido, String genero,
                                   String paisResidencia, String ciudadResidencia, String ciudadProcedencia,
                                   String ciudadDestino, String motivoViaje, List<String> datosTraFaltantes,
                                   boolean principal) {
    }

    public static ReservaResponse de(ReservaResult r) {
        return de(r, null, null, null, null);
    }

    public static ReservaResponse de(ReservaConTitularResult r) {
        return de(r.reserva(), r.titularNombre(), r.anticipoExigido(),
                Optional.ofNullable(r.noShow())
                        .map(n -> new DeclaracionNoShowResponse(n.autor(), n.autorNombre(), n.fechaHora()))
                        .orElse(null),
                Optional.ofNullable(r.correccionesDocumento())
                        .map(lista -> lista.stream()
                                .map(c -> new CorreccionDocumentoResponse(c.ocupante(), c.tipoAnterior(),
                                        c.numeroAnterior(), c.tipoNuevo(), c.numeroNuevo(), c.motivo(), c.autor(),
                                        c.autorNombre(), c.fechaHora(), c.titular()))
                                .toList())
                        .orElse(null));
    }

    private static ReservaResponse de(ReservaResult r, String titularNombre, BigDecimal anticipoExigido,
                                      DeclaracionNoShowResponse noShow,
                                      List<CorreccionDocumentoResponse> correccionesDocumento) {
        return new ReservaResponse(r.codigo(), r.apartamento(), r.titularId(), r.entrada(), r.salida(), r.estado(),
                r.canalOrigen(),
                r.ocupantes().stream()
                        .map(o -> new OcupanteResponse(o.id(), o.nombre(), o.fechaNacimiento(), o.tipoDocumento(),
                                o.numeroDocumento(), o.nacionalidad(), o.nombres(), o.primerApellido(),
                                o.segundoApellido(), o.genero(), o.paisResidencia(), o.ciudadResidencia(),
                                o.ciudadProcedencia(), o.ciudadDestino(), o.motivoViaje(), o.datosTraFaltantes(),
                                o.principal()))
                        .toList(),
                r.horaEstimadaLlegada(), CotizacionResponse.de(r.valor()), r.politicaId(), r.creadaEn(),
                r.avisoCuenta(), r.vehiculos(), r.mascotas(), titularNombre, r.carros(), r.motos(),
                new ExtrasResponse(r.extras().parqueadero(), r.extras().mascotas(), r.extras().total()),
                anticipoExigido, noShow, correccionesDocumento);
    }
}
