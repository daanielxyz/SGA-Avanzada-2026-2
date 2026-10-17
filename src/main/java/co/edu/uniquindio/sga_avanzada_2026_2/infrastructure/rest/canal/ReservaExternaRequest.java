package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RecibirReservaExternaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva.OcupanteRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpo de {@code POST /api/canales/{id}/reservas}: el contrato con el que un canal externo envía una reserva
 * (CU-12). No trae precio: lo calcula el sistema con sus tarifas (DEC-62).
 *
 * @param idExterno identificador de la reserva en el canal; repetirlo no crea otra reserva (RN-19)
 * @param salida    exclusiva: esa noche no se ocupa (EST-01)
 * @param ocupantes el grupo completo, incluido el titular con su documento (TIT-01)
 */
public record ReservaExternaRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 80, message = "admite máximo 80 caracteres")
        String idExterno,

        @NotBlank(message = "es obligatorio")
        String apartamento,

        @NotNull(message = "es obligatoria")
        LocalDate entrada,

        @NotNull(message = "es obligatoria")
        LocalDate salida,

        @NotNull(message = "es obligatorio")
        @Valid
        TitularExternoRequest titular,

        @NotEmpty(message = "debe tener al menos un ocupante")
        List<@Valid @NotNull(message = "no puede ser nulo") OcupanteRequest> ocupantes) {

    /**
     * @param cargaUtil este mismo cuerpo como texto, para la bitácora; no contiene credenciales (BIT-04)
     */
    RecibirReservaExternaCommand aCommand(String canalId, String cargaUtil) {
        return new RecibirReservaExternaCommand(canalId, idExterno, apartamento, entrada, salida,
                titular.aCommand(), OcupanteRequest.aCommands(ocupantes), cargaUtil);
    }
}
