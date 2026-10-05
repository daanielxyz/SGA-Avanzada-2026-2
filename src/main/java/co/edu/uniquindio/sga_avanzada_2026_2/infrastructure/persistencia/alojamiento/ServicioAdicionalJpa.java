package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.alojamiento;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Fila de {@code alojamiento_servicio_adicional}.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServicioAdicionalJpa {

    private String id;
    private String nombre;
    private boolean generaCargo;
    private BigDecimal valor;
    private boolean activo;
}
