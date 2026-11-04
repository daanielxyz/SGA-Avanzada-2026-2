package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado Apartamento. Guarda y carga el agregado completo (raíz + bloqueos,
 * imágenes y características).
 */
public interface ApartamentoRepository {

    void guardar(Apartamento apartamento);

    Optional<Apartamento> buscarPorCodigo(ApartamentoId codigo);

    /** Todos los apartamentos del alojamiento, activos o no, ordenados por código (CU-31). */
    Pagina<Apartamento> listarPorAlojamiento(AlojamientoId alojamientoId, int numeroPagina);

    /** Los apartamentos en venta: los que exigen tarifa en toda temporada (TAR-03) y variedad de capacidad (CAP-05). */
    List<Apartamento> buscarActivos(AlojamientoId alojamientoId);
}
