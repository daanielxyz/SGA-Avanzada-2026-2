package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import java.util.Optional;
import jakarta.validation.constraints.Min;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CrearReservaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Cuerpo de {@code POST /api/reservas} (CU-10 · CU-11). Valida solo el formato (400); disponibilidad, capacidad y las
 * demás reglas las valida el dominio (409). Las reservas de un canal externo entran por el contrato del canal
 * (DEC-62).
 *
 * @param salida              exclusiva: esa noche no se ocupa (EST-01)
 * @param ocupantes           quienes se alojan; el titular puede no estar entre ellos (TIT-01 · DEC-97)
 * @param horaEstimadaLlegada opcional al crear; necesaria para confirmar (RN-09)
 */
public record CrearReservaRequest(
        @NotBlank(message = "es obligatorio")
        String apartamento,

        @NotNull(message = "es obligatoria")
        LocalDate entrada,

        @NotNull(message = "es obligatoria")
        LocalDate salida,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "PORTAL|DIRECTO", message = "debe ser PORTAL o DIRECTO")
        String canalOrigen,

        @NotNull(message = "es obligatorio")
        @Valid
        TitularReservaRequest titular,

        @NotEmpty(message = "debe tener al menos un ocupante")
        List<@Valid @NotNull(message = "no puede ser nulo") OcupanteRequest> ocupantes,

        LocalTime horaEstimadaLlegada,

        // Opcionales: sin ellos, ni vehículos ni mascotas (RP-02 · RP-03)
        @Min(value = 0, message = "no puede ser negativo")
        Integer carros,

        @Min(value = 0, message = "no puede ser negativo")
        Integer motos,

        // TODO(equipo): se quita cuando recepción envíe carros y motos; mientras tanto, cuenta como carros (DEC-89)
        @Min(value = 0, message = "no puede ser negativo")
        Integer vehiculos,

        @Min(value = 0, message = "no puede ser negativo")
        Integer mascotas) {

    CrearReservaCommand aCommand() {
        return new CrearReservaCommand(apartamento, entrada, salida, canalOrigen, titular.aCommand(),
                OcupanteRequest.aCommands(ocupantes), horaEstimadaLlegada,
                Optional.ofNullable(carros).or(() -> Optional.ofNullable(vehiculos)).orElse(0),
                Optional.ofNullable(motos).orElse(0), Optional.ofNullable(mascotas).orElse(0));
    }
}
