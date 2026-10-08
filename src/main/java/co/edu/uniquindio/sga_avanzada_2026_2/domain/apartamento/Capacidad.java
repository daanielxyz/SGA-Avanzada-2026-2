package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.util.List;

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

    /**
     * Exige variedad de capacidades entre los apartamentos activos, según la Ficha del curso (CAP-05 · DEC-51): si
     * hay al menos {@code minimoDistintas} activos, deben tener al menos ese número de capacidades distintas. Con
     * menos activos todavía no se puede cumplir y no se exige.
     *
     * @param capacidadesActivas capacidades de los apartamentos activos, incluido el que se activa o cambia
     * @param minimoDistintas    {@code ParametrosAlojamiento.minimoCapacidadesDistintas}
     * @throws ReglaDominioException si hay activos suficientes y no tienen las capacidades distintas exigidas
     */
    public static void exigirVariedad(List<Capacidad> capacidadesActivas, int minimoDistintas) {
        long distintas = capacidadesActivas.stream().distinct().count();
        if (capacidadesActivas.size() >= minimoDistintas && distintas < minimoDistintas) {
            throw new ReglaDominioException("Los apartamentos activos deben tener al menos " + minimoDistintas
                    + " capacidades distintas");
        }
    }
}
