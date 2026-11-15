package co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;

import java.util.Set;

/**
 * VO: qué novedades listar en la lista general (DEC-132). Cada filtro admite varios valores («cualquiera de
 * ellos»); entre filtros distintos es «y», como en Reservas (DEC-119). Un filtro vacío es «cualquiera».
 */
public record FiltroNovedades(Set<EstadoNovedad> estados, Set<GravedadNovedad> gravedades,
                              Set<ApartamentoId> apartamentos) {

    public FiltroNovedades {
        estados = estados == null ? Set.of() : Set.copyOf(estados);
        gravedades = gravedades == null ? Set.of() : Set.copyOf(gravedades);
        apartamentos = apartamentos == null ? Set.of() : Set.copyOf(apartamentos);
    }

    /** Sin filtros: todas. */
    public static FiltroNovedades todas() {
        return new FiltroNovedades(Set.of(), Set.of(), Set.of());
    }

    /** Si la novedad entra en la lista: su estado actual, su gravedad y su apartamento cumplen cada filtro. */
    public boolean cumple(Novedad novedad) {
        return (estados.isEmpty() || estados.contains(novedad.estado()))
                && (gravedades.isEmpty() || gravedades.contains(novedad.gravedad()))
                && (apartamentos.isEmpty() || apartamentos.contains(novedad.apartamentoId()));
    }
}
