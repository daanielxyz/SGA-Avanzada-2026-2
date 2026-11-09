package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tabla del agregado Titular. El documento es único: un titular conserva su identidad entre reservas (TIT-05).
 */
@Entity
@Table(name = "titular")
@Getter
@Setter
@NoArgsConstructor
public class TitularJpa {

    @Id
    private String id;

    private String alojamientoId;
    private String nombres;
    private String primerApellido;
    private String segundoApellido;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;

    private String numeroDocumento;
    private String correo;
    private String telefono;
    private String validadoPor;        // DEC-98: null si recepción no la ha validado
    private LocalDateTime validadoEn;

    @Version
    private Long version;
}
