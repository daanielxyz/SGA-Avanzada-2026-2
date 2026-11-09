package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Genero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.MotivoViaje;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Fila de {@code reserva_ocupante}. En el dominio Ocupante es entidad interna; aquí basta un embebido porque solo
 * existe dentro de su reserva.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OcupanteJpa {

    private String id;
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento; // null si no se registró documento

    private String numeroDocumento;
    private String nacionalidad;

    @Enumerated(EnumType.STRING)
    private Genero genero;

    private String paisResidencia;
    private String ciudadResidencia;
    private String ciudadProcedencia;
    private String ciudadDestino;

    @Enumerated(EnumType.STRING)
    private MotivoViaje motivoViaje;

    private boolean principal; // DEC-97
}
