package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fila de {@code reserva_registro}: un registro de llegada, vigente o anulado (REG-06 · DEC-45).
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroJpa {

    private String id;
    private LocalDateTime fechaHora;
    private String autor;
    private boolean anulado;
    private String motivoCorreccion; // solo en una corrección
}
