package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tarifa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila de {@code tarifa}: una por versión. Sin setters ni {@code @Version} porque una versión nunca se modifica
 * (TAR-05); la restricción única (apartamento, temporada, versión) impide duplicarla.
 */
@Entity
@Table(name = "tarifa")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TarifaJpa {

    @Id
    private String id;

    private String apartamentoCodigo;
    private String temporadaId;
    private BigDecimal valorPorOcupante;
    private int version;
    private LocalDate vigenteDesde;
}
