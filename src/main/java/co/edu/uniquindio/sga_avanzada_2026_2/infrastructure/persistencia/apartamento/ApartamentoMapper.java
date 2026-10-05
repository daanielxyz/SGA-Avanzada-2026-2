package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Bloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.BloqueoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;

/**
 * Convierte Apartamento ⇄ ApartamentoJpa. Al cargar usa el constructor de reconstrucción del dominio, que
 * vuelve a validar las invariantes (DEC-06).
 */
final class ApartamentoMapper {

    private ApartamentoMapper() {
    }

    static Apartamento aDominio(ApartamentoJpa jpa) {
        return new Apartamento(
                new ApartamentoId(jpa.getCodigo()),
                new AlojamientoId(jpa.getAlojamientoId()),
                jpa.getNombre(),
                jpa.getDescripcion(),
                new Capacidad(jpa.getCapacidad()),
                new Dormitorio(jpa.getDormitorios()),
                jpa.getEstadoOperativo(),
                jpa.getImagenes().stream().map(i -> new Imagen(i.getUrl(), i.isPrincipal())).toList(),
                jpa.getCaracteristicas().stream().map(Caracteristica::new).toList(),
                jpa.getBloqueos().stream()
                        .map(b -> new Bloqueo(new BloqueoId(b.getId()), b.getFechaInicio(), b.getFechaFin(),
                                b.getMotivo(), b.isVigente()))
                        .toList(),
                jpa.isActivo());
    }

    /**
     * Copia el estado del dominio sobre una fila nueva o ya cargada; no toca {@code version}, así el bloqueo
     * optimista sigue funcionando.
     */
    static void copiar(Apartamento apartamento, ApartamentoJpa jpa) {
        jpa.setCodigo(apartamento.codigo().valor());
        jpa.setAlojamientoId(apartamento.alojamientoId().valor());
        jpa.setNombre(apartamento.nombre());
        jpa.setDescripcion(apartamento.descripcion());
        jpa.setCapacidad(apartamento.capacidad().valor());
        jpa.setDormitorios(apartamento.dormitorios().cantidad());
        jpa.setEstadoOperativo(apartamento.estadoOperativo());
        jpa.setActivo(apartamento.activo());
        jpa.getImagenes().clear();
        apartamento.imagenes().forEach(i -> jpa.getImagenes().add(new ImagenJpa(i.url(), i.principal())));
        jpa.getCaracteristicas().clear();
        apartamento.caracteristicas().forEach(c -> jpa.getCaracteristicas().add(c.nombre()));
        jpa.getBloqueos().clear();
        apartamento.bloqueos().forEach(b -> jpa.getBloqueos().add(
                new BloqueoJpa(b.id().valor(), b.fechaInicio(), b.fechaFin(), b.motivo(), b.vigente())));
    }
}
