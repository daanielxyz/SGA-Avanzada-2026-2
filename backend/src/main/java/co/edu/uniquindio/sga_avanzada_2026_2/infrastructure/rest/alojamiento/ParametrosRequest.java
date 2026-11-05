package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import jakarta.validation.constraints.DecimalMin;
import java.util.Optional;
import java.math.BigDecimal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.CambiarParametrosAlojamientoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.ParametrosCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;

/**
 * Cuerpo de {@code PUT /api/alojamiento/parametros}: reemplaza todos los parámetros a la vez. Valida solo el formato
 * (400); que sean coherentes entre sí lo valida el dominio (409) (DEC-31).
 */
public record ParametrosRequest(
        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer umbralEdadFacturable,

        @NotNull(message = "es obligatoria")
        LocalTime horaEntrada,

        @NotNull(message = "es obligatoria")
        LocalTime horaSalida,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer tiempoPreparacionHoras,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer plazoConfirmacionHoras,

        @NotNull(message = "es obligatoria")
        LocalTime horaLimiteNoShow,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "debe estar entre 0 y 100")
        @Max(value = 100, message = "debe estar entre 0 y 100")
        Integer anticipoPct,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer minimoMediosPago,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer minimoServiciosAdicionales,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer minimoTemporadas,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer minimoTramosCancelacion,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer minimoCapacidadesDistintas,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "ANTICIPO|TOTAL", message = "debe ser ANTICIPO o TOTAL")
        String pagoEnLinea,

        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        Integer plazoPagoEnLineaMinutos,

        // Opcionales (RP-02 · RP-03 · L-16): sin ellos, el alojamiento queda sin parqueadero ni mascotas
        @Min(value = 0, message = "no puede ser negativo")
        Integer cupoParqueadero,

        Boolean aceptaMascotas,

        @Min(value = 0, message = "no puede ser negativo")
        Integer limiteMascotas,

        @DecimalMin(value = "0", message = "no puede ser negativo")
        BigDecimal cargoMascota,

        // Opcionales (DEC-89): sin ellos, el parqueadero no se cobra
        @Pattern(regexp = "POR_NOCHE|POR_ESTADIA", message = "debe ser POR_NOCHE o POR_ESTADIA")
        String cobroParqueadero,

        @DecimalMin(value = "0", message = "no puede ser negativo")
        BigDecimal cargoCarro,

        @DecimalMin(value = "0", message = "no puede ser negativo")
        BigDecimal cargoMoto) {

    CambiarParametrosAlojamientoCommand aCommand(String alojamientoId) {
        return new CambiarParametrosAlojamientoCommand(alojamientoId, new ParametrosCommand(umbralEdadFacturable,
                horaEntrada, horaSalida, tiempoPreparacionHoras, plazoConfirmacionHoras, horaLimiteNoShow,
                anticipoPct, minimoMediosPago, minimoServiciosAdicionales, minimoTemporadas,
                minimoTramosCancelacion, minimoCapacidadesDistintas, pagoEnLinea, plazoPagoEnLineaMinutos,
                Optional.ofNullable(cupoParqueadero).orElse(0), Boolean.TRUE.equals(aceptaMascotas),
                Optional.ofNullable(limiteMascotas).orElse(0), Optional.ofNullable(cargoMascota).orElse(BigDecimal.ZERO),
                Optional.ofNullable(cobroParqueadero).orElse("POR_NOCHE"),
                Optional.ofNullable(cargoCarro).orElse(BigDecimal.ZERO),
                Optional.ofNullable(cargoMoto).orElse(BigDecimal.ZERO)));
    }
}
