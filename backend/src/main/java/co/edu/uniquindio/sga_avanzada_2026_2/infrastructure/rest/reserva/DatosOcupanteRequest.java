package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CompletarDatosOcupanteCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/reservas/{codigo}/ocupantes/{ocupanteId}/datos-tra}: los datos del TRA de un ocupante
 * (BL-07). Todo es opcional: lo que no llega se conserva, para guardar por partes.
 */
public record DatosOcupanteRequest(
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombres,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String primerApellido,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String segundoApellido,

        @Pattern(regexp = "CC|CE|PASAPORTE|TI", message = "debe ser CC, CE, PASAPORTE o TI")
        String tipoDocumento,

        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String nacionalidad,

        @Pattern(regexp = "FEMENINO|MASCULINO|OTRO", message = "debe ser FEMENINO, MASCULINO u OTRO")
        String genero,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String paisResidencia,

        @Size(max = 80, message = "admite máximo 80 caracteres")
        String ciudadResidencia,

        @Size(max = 80, message = "admite máximo 80 caracteres")
        String ciudadProcedencia,

        @Size(max = 80, message = "admite máximo 80 caracteres")
        String ciudadDestino,

        @Pattern(regexp = "TURISMO_VACACIONES|EVENTOS|ESTUDIOS|TRATAMIENTO_MEDICO|ASUNTOS_JURIDICOS|NEGOCIOS|TRANSITO"
                + "|COMPRAS|RELIGION|VISITA_FAMILIARES_AMIGOS", message = "no es un motivo de viaje del TRA")
        String motivoViaje) {

    /** El documento llega completo o no llega: el tipo sin el número, o al revés, no sirve. */
    @JsonIgnore
    @AssertTrue(message = "tipoDocumento y numeroDocumento van juntos")
    public boolean isDocumentoCompleto() {
        return (tipoDocumento == null) == (numeroDocumento == null || numeroDocumento.isBlank());
    }

    CompletarDatosOcupanteCommand aCommand(String codigo, String ocupanteId) {
        return new CompletarDatosOcupanteCommand(codigo, ocupanteId, nombres, primerApellido, segundoApellido,
                tipoDocumento, numeroDocumento, nacionalidad, genero, paisResidencia, ciudadResidencia,
                ciudadProcedencia, ciudadDestino, motivoViaje);
    }
}
