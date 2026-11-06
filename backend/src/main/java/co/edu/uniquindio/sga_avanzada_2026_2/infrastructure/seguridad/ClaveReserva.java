package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * La huella de la clave de una reserva del portal, con su tabla (V18 · DEC-93). Nunca la clave.
 */
@Entity
@Table(name = "clave_reserva")
public class ClaveReserva {

    @Id
    private String reservaCodigo;

    private String huella;
    private LocalDateTime creadaEn;

    protected ClaveReserva() {
    }

    ClaveReserva(String reservaCodigo, String huella, LocalDateTime creadaEn) {
        this.reservaCodigo = reservaCodigo;
        this.huella = huella;
        this.creadaEn = creadaEn;
    }

    String huella() {
        return huella;
    }
}
