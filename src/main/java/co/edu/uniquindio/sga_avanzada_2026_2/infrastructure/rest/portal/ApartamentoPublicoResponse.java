package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ApartamentoResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CotizacionResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * JSON de un apartamento para el visitante del portal: fotos, descripción, capacidad, dormitorios y dotación
 * ({@code caracteristicas}). Deja fuera lo de la operación (estado operativo, bloqueos y sus motivos, si está activo),
 * que solo ve el personal en {@code ApartamentoResponse} (DEC-67). Qué ve el público es un supuesto a validar con el
 * cliente en el cierre 1: agregar campos aquí no rompe a nadie.
 *
 * @param precio en la búsqueda, lo que cuesta la estancia buscada para el grupo; en la ficha, sin fechas, va vacío
 */
public record ApartamentoPublicoResponse(String codigo, String nombre, String descripcion, int capacidad,
                                         int dormitorios, List<ImagenResponse> imagenes,
                                         List<String> caracteristicas, PrecioResponse precio) {

    /**
     * @param principal la foto que se muestra primero
     */
    public record ImagenResponse(String url, boolean principal) {
    }

    /** El desglose noche por noche y el total (COT-02), sin los ids internos de las temporadas. */
    public record PrecioResponse(List<NocheResponse> desglose, BigDecimal total) {
    }

    /**
     * @param tarifa lo que paga cada ocupante facturable esa noche, según su temporada
     */
    public record NocheResponse(LocalDate noche, BigDecimal tarifa, int ocupantesFacturables, BigDecimal subtotal) {
    }

    static ApartamentoPublicoResponse de(ApartamentoResult r) {
        return de(r, null);
    }

    static ApartamentoPublicoResponse de(ApartamentoResult r, CotizacionResult precio) {
        return new ApartamentoPublicoResponse(r.codigo(), r.nombre(), r.descripcion(), r.capacidad(),
                r.dormitorios(), r.imagenes().stream().map(i -> new ImagenResponse(i.url(), i.principal())).toList(),
                r.caracteristicas(), precio == null ? null : new PrecioResponse(precio.desglose().stream()
                        .map(n -> new NocheResponse(n.noche(), n.tarifa(), n.ocupantesFacturables(), n.subtotal()))
                        .toList(), precio.total()));
    }
}
