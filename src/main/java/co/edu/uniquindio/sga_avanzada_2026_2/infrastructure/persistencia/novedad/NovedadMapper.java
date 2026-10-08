package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.CambioEstadoNovedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.Novedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadId;

/**
 * Convierte Novedad ⇄ NovedadJpa. Al cargar usa el constructor del dominio, que valida el orden del historial
 * (DEC-06).
 */
final class NovedadMapper {

    private NovedadMapper() {
    }

    static Novedad aDominio(NovedadJpa jpa) {
        return new Novedad(new NovedadId(jpa.getId()), new ApartamentoId(jpa.getApartamentoCodigo()),
                jpa.getDescripcion(), jpa.getGravedad(),
                jpa.getHistorial().stream()
                        .map(c -> new CambioEstadoNovedad(c.getEstado(), new UsuarioId(c.getAutor()), c.getFechaHora()))
                        .toList());
    }

    /** Copia sobre una fila nueva o ya cargada; no toca {@code version}. */
    static void copiar(Novedad novedad, NovedadJpa jpa) {
        jpa.setId(novedad.id().valor());
        jpa.setApartamentoCodigo(novedad.apartamentoId().valor());
        jpa.setDescripcion(novedad.descripcion());
        jpa.setGravedad(novedad.gravedad());
        jpa.getHistorial().clear();
        novedad.historial().forEach(c -> jpa.getHistorial().add(
                new CambioNovedadJpa(c.estado(), c.autor().valor(), c.fechaHora())));
    }
}
