package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ApartamentoResult;

import java.time.LocalDate;
import java.util.List;

/**
 * JSON de un apartamento hacia el cliente (DEC-31). Hoy coincide con {@link ApartamentoResult}; existe aparte
 * para que el contrato HTTP pueda cambiar sin tocar los casos de uso.
 */
public record ApartamentoResponse(String codigo, String alojamientoId, String nombre, String descripcion,
                                  int capacidad, int dormitorios, String estadoOperativo, boolean activo,
                                  List<ImagenResponse> imagenes, List<String> caracteristicas,
                                  List<BloqueoResponse> bloqueos) {

    public record ImagenResponse(String url, boolean principal) {
    }

    public record BloqueoResponse(String id, LocalDate fechaInicio, LocalDate fechaFin, String motivo,
                                  boolean vigente) {
    }

    static ApartamentoResponse de(ApartamentoResult r) {
        return new ApartamentoResponse(r.codigo(), r.alojamientoId(), r.nombre(), r.descripcion(), r.capacidad(),
                r.dormitorios(), r.estadoOperativo(), r.activo(),
                r.imagenes().stream().map(i -> new ImagenResponse(i.url(), i.principal())).toList(),
                r.caracteristicas(),
                r.bloqueos().stream()
                        .map(b -> new BloqueoResponse(b.id(), b.fechaInicio(), b.fechaFin(), b.motivo(), b.vigente()))
                        .toList());
    }
}
