package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ApartamentoResult;

import java.util.List;

/**
 * JSON de un apartamento para el visitante del portal: fotos, descripción, capacidad, dormitorios y dotación
 * ({@code caracteristicas}). Deja fuera lo de la operación (estado operativo, bloqueos y sus motivos, si está activo),
 * que solo ve el personal en {@code ApartamentoResponse} (DEC-67). Qué ve el público es un supuesto a validar con el
 * cliente en el cierre 1: agregar campos aquí no rompe a nadie.
 */
public record ApartamentoPublicoResponse(String codigo, String nombre, String descripcion, int capacidad,
                                         int dormitorios, List<ImagenResponse> imagenes,
                                         List<String> caracteristicas) {

    /**
     * @param principal la foto que se muestra primero
     */
    public record ImagenResponse(String url, boolean principal) {
    }

    static ApartamentoPublicoResponse de(ApartamentoResult r) {
        return new ApartamentoPublicoResponse(r.codigo(), r.nombre(), r.descripcion(), r.capacidad(),
                r.dormitorios(), r.imagenes().stream().map(i -> new ImagenResponse(i.url(), i.principal())).toList(),
                r.caracteristicas());
    }
}
