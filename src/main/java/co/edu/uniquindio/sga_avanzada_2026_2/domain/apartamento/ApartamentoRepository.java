package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import java.util.Optional;

/**
 * Puerto de persistencia del agregado Apartamento. Guarda y carga el agregado completo (raíz + bloqueos,
 * imágenes y características).
 */
public interface ApartamentoRepository {

    void guardar(Apartamento apartamento);

    Optional<Apartamento> buscarPorCodigo(ApartamentoId codigo);
}
