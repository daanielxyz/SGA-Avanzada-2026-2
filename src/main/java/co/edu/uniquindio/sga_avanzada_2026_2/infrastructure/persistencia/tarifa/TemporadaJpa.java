package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tarifa;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Fila de {@code temporada}.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TemporadaJpa {

    private String id;
    private String nombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private boolean esBase;
    private int estanciaMinimaNoches;
    private boolean activa;
}
