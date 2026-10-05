package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.apartamento;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Fila de {@code bloqueo}. En el dominio Bloqueo es entidad interna; aquí basta un embebido porque solo
 * existe dentro de su apartamento.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BloqueoJpa {

    private String id;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String motivo;
    private boolean vigente;
}
