package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;

import java.time.LocalDate;
import java.util.List;

/**
 * Salida de los casos de uso de Apartamento (DEC-31): una foto de datos simples, nunca la entidad, para que quien
 * la reciba no pueda saltarse el caso de uso.
 */
public record ApartamentoResult(String codigo, String alojamientoId, String nombre, String descripcion,
                                int capacidad, int dormitorios, String estadoOperativo, boolean activo,
                                List<ImagenResult> imagenes, List<String> caracteristicas,
                                List<BloqueoResult> bloqueos) {

    public record ImagenResult(String url, boolean principal) {
    }

    public record BloqueoResult(String id, LocalDate fechaInicio, LocalDate fechaFin, String motivo,
                                boolean vigente) {
    }

    static ApartamentoResult de(Apartamento apartamento) {
        return new ApartamentoResult(
                apartamento.codigo().valor(),
                apartamento.alojamientoId().valor(),
                apartamento.nombre(),
                apartamento.descripcion(),
                apartamento.capacidad().valor(),
                apartamento.dormitorios().cantidad(),
                apartamento.estadoOperativo().name(),
                apartamento.activo(),
                apartamento.imagenes().stream().map(i -> new ImagenResult(i.url(), i.principal())).toList(),
                apartamento.caracteristicas().stream().map(Caracteristica::nombre).toList(),
                apartamento.bloqueos().stream()
                        .map(b -> new BloqueoResult(b.id().valor(), b.fechaInicio(), b.fechaFin(), b.motivo(),
                                b.vigente()))
                        .toList());
    }
}
