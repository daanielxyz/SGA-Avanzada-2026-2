package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla del agregado Canal. Guarda la referencia a la credencial, nunca el secreto (CAN-02 · DEC-47).
 */
@Entity
@Table(name = "canal")
@Getter
@Setter
@NoArgsConstructor
public class CanalJpa {

    @Id
    private String id;

    private String alojamientoId;
    private String nombre;

    @Enumerated(EnumType.STRING)
    private CanalOrigen tipo;

    private String referenciaCredencial;
    private boolean activo;

    @Version
    private Long version;
}
