package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: cantidad de dormitorios. Es descriptivo: nunca se reserva ni se cobra por separado (DOR-01) y no
 * determina la capacidad (DOR-03).
 */
public record Dormitorio(int cantidad) {

    // DOR-02
    public Dormitorio {
        if (cantidad < 1) {
            throw new ReglaDominioException("Debe haber al menos un dormitorio");
        }
    }
}
