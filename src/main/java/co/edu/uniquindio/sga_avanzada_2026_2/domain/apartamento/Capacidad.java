package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: número máximo de ocupantes del apartamento (CAP-01). Inmutable: cambiarla reemplaza el valor (CAP-04).
 */
public record Capacidad(int valor) {

    // CAP-01
    public Capacidad {
        if (valor < 1) {
            throw new ReglaDominioException("La capacidad debe ser al menos 1");
        }
    }

    /**
     * Tope rígido de ocupantes, sin excepciones ni autorizaciones (RN-02 · CAP-02). Cuentan todos los ocupantes,
     * sean facturables o no (CAP-03).
     *
     * @param ocupantes total de personas del grupo
     * @return {@code true} si el grupo cabe en el apartamento
     */
    public boolean admite(int ocupantes) {
        return ocupantes <= valor;
    }
}
