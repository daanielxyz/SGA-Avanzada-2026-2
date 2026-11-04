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
 * @param precio            en la búsqueda por fechas, lo que cuesta esa estancia para el grupo; en el listado y la
 *                          ficha, lo que paga un adulto su próxima noche libre; vacío si no tiene ninguna
 * @param proximaNocheLibre en el listado y la ficha, la primera noche que se puede reservar desde hoy; en la búsqueda
 *                          por fechas va vacía, porque esas fechas ya están libres
 */
public record ApartamentoPublicoResponse(String codigo, String nombre, String descripcion, int capacidad,
                                         int dormitorios, List<ImagenResponse> imagenes,
                                         List<String> caracteristicas, PrecioResponse precio,
                                         LocalDate proximaNocheLibre) {

    /**
     * @param principal la foto que se muestra primero
     */
    public record ImagenResponse(String url, boolean principal) {
    }

    /** El desglose noche por noche y el total (COT-02), sin los ids internos de las temporadas. */
    public record PrecioResponse(List<NocheResponse> desglose, BigDecimal total) {

        static PrecioResponse de(CotizacionResult r) {
            return r == null ? null : new PrecioResponse(r.desglose().stream()
                    .map(n -> new NocheResponse(n.noche(), n.tarifa(), n.ocupantesFacturables(), n.subtotal()))
                    .toList(), r.total());
        }
    }

    /**
     * @param tarifa lo que paga cada ocupante facturable esa noche, según su temporada
     */
    public record NocheResponse(LocalDate noche, BigDecimal tarifa, int ocupantesFacturables, BigDecimal subtotal) {
    }

    static ApartamentoPublicoResponse de(ApartamentoResult r, CotizacionResult precio, LocalDate proximaNocheLibre) {
        return new ApartamentoPublicoResponse(r.codigo(), r.nombre(), r.descripcion(), r.capacidad(),
                r.dormitorios(), r.imagenes().stream().map(i -> new ImagenResponse(i.url(), i.principal())).toList(),
                r.caracteristicas(), PrecioResponse.de(precio), proximaNocheLibre);
    }
}
