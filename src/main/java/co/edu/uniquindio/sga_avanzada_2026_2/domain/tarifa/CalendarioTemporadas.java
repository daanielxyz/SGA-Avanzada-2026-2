package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado CalendarioTemporadas: uno por alojamiento (DEC-28). Garantiza que exista exactamente una
 * temporada base (TEM-08), que las específicas no se solapen (TEM-02) y que toda noche tenga temporada (TEM-03).
 */
public class CalendarioTemporadas {

    private final AlojamientoId alojamientoId;
    private final List<Temporada> temporadas;

    // TEM-02 · TEM-08
    public CalendarioTemporadas(AlojamientoId alojamientoId, List<Temporada> temporadas) {
        if (alojamientoId == null || temporadas == null) {
            throw new ReglaDominioException("El calendario requiere alojamiento y temporadas");
        }
        if (temporadas.stream().filter(Temporada::esBase).count() != 1) {
            throw new ReglaDominioException("El calendario debe tener exactamente una temporada base");
        }
        if (temporadas.stream().map(Temporada::id).distinct().count() != temporadas.size()) {
            throw new ReglaDominioException("Hay temporadas con id repetido");
        }
        for (int i = 0; i < temporadas.size(); i++) {
            for (int j = i + 1; j < temporadas.size(); j++) {
                if (temporadas.get(i).seSolapaCon(temporadas.get(j))) {
                    throw new ReglaDominioException("Las temporadas " + temporadas.get(i).id().valor() + " y "
                            + temporadas.get(j).id().valor() + " se solapan");
                }
            }
        }
        this.alojamientoId = alojamientoId;
        this.temporadas = new ArrayList<>(temporadas);
    }

    /**
     * Agrega una temporada específica activa. Reduce la cobertura de la base sin editarla (TEM-11). Que nazca con
     * las tarifas de todos los apartamentos activos (TAR-03) lo coordina el caso de uso.
     *
     * @param estanciaMinimaNoches 0 si no exige mínimo (TEM-07)
     * @throws ReglaDominioException si el id ya existe, el rango es inválido (TEM-01) o se solapa con otra (TEM-02)
     */
    public void agregarTemporada(TemporadaId id, String nombre, LocalDate inicio, LocalDate fin,
                                 int estanciaMinimaNoches) {
        Temporada nueva = new Temporada(id, nombre, inicio, fin, false, estanciaMinimaNoches, true);
        if (temporadas.contains(nueva)) {
            throw new ReglaDominioException("Ya existe la temporada " + id.valor());
        }
        validarSinSolape(nueva);
        temporadas.add(nueva);
    }

    /**
     * Cambia el rango de una temporada específica; las reservas ya creadas no cambian (TEM-05).
     *
     * @throws ReglaDominioException si no existe, es la base o el nuevo rango se solapa con otra (TEM-02)
     */
    public void cambiarFechas(TemporadaId id, LocalDate inicio, LocalDate fin) {
        Temporada temporada = temporada(id);
        Temporada propuesta = new Temporada(id, temporada.nombre(), inicio, fin, temporada.esBase(),
                temporada.estanciaMinimaNoches(), temporada.activa());
        validarSinSolape(propuesta);
        temporada.cambiarFechas(inicio, fin);
    }

    /**
     * Desactiva una temporada específica; sus fechas vuelven a la base (TEM-06 · TEM-11).
     *
     * @throws ReglaDominioException si no existe, ya estaba inactiva o es la base (TEM-10)
     */
    public void desactivarTemporada(TemporadaId id) {
        temporada(id).desactivar();
    }

    /**
     * Temporada que aplica a una noche: la específica activa que la cubre o, si ninguna, la base. Ninguna noche
     * queda sin temporada (TEM-03 · TEM-09 · NOC-04).
     */
    public Temporada temporadaDe(Noche noche) {
        return temporadas.stream()
                .filter(t -> t.cubre(noche))
                .findFirst()
                .orElseGet(this::base);
    }

    /**
     * Estancia mínima que exige una estancia: la mayor de las temporadas que toca al menos una de sus noches
     * (RP-01 · TEM-07). El resultado se pasa a {@code Reserva.crear/modificar}.
     *
     * @return noches mínimas; 0 si ninguna temporada de la estancia exige mínimo
     */
    public int estanciaMinimaPara(Estancia estancia) {
        Objects.requireNonNull(estancia, "estancia");
        return estancia.noches().stream()
                .mapToInt(noche -> temporadaDe(noche).estanciaMinimaNoches())
                .max()
                .orElse(0);
    }

    /**
     * Temporadas en uso (activas, incluida la base): cada una necesita tarifa por apartamento activo (TAR-03).
     */
    public List<Temporada> temporadasActivas() {
        return temporadas.stream().filter(Temporada::activa).toList();
    }

    /**
     * Cantidad de temporadas específicas activas, para el mínimo configurable de la Ficha (TEM-04).
     */
    public long cantidadTemporadasEspecificas() {
        return temporadas.stream().filter(t -> t.activa() && !t.esBase()).count();
    }

    private void validarSinSolape(Temporada candidata) {
        temporadas.stream()
                .filter(t -> !t.equals(candidata) && t.seSolapaCon(candidata))
                .findFirst()
                .ifPresent(t -> {
                    throw new ReglaDominioException("La temporada " + candidata.id().valor()
                            + " se solapa con " + t.id().valor());
                });
    }

    private Temporada base() {
        return temporadas.stream().filter(Temporada::esBase).findFirst().orElseThrow();
    }

    private Temporada temporada(TemporadaId id) {
        Objects.requireNonNull(id, "id");
        return temporadas.stream()
                .filter(t -> t.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ReglaDominioException("El calendario no tiene la temporada " + id.valor()));
    }

    public AlojamientoId alojamientoId() {
        return alojamientoId;
    }

    public List<Temporada> temporadas() {
        return List.copyOf(temporadas);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CalendarioTemporadas otro && alojamientoId.equals(otro.alojamientoId);
    }

    @Override
    public int hashCode() {
        return alojamientoId.hashCode();
    }
}
