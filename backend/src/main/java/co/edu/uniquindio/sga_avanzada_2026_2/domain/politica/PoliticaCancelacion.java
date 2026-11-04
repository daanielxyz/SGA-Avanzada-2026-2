package co.edu.uniquindio.sga_avanzada_2026_2.domain.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.util.Comparator;
import java.util.List;

/**
 * Raíz del agregado PoliticaCancelacion: una instancia por versión, inmutable. Cada alojamiento define sus tramos y
 * la consecuencia del no-show (DEC-41); la vigente es la versión más alta (POL-01 · DEC-42) y cada reserva congela la
 * que aceptó al reservar (RN-22 · POL-04).
 */
public class PoliticaCancelacion {

    private final PoliticaId id;
    private final AlojamientoId alojamientoId;
    private final int version;
    private final List<TramoCancelacion> tramos; // ordenados por antelación, el primero desde 0 h
    private final Penalizacion penalizacionNoShow;

    // POL-02 · POL-06 · TRAM-02
    public PoliticaCancelacion(PoliticaId id, AlojamientoId alojamientoId, int version,
                               List<TramoCancelacion> tramos, Penalizacion penalizacionNoShow) {
        if (id == null || alojamientoId == null || tramos == null || penalizacionNoShow == null) {
            throw new ReglaDominioException("Faltan datos obligatorios de la política de cancelación");
        }
        if (version < 1) {
            throw new ReglaDominioException("La versión de la política empieza en 1");
        }
        List<TramoCancelacion> ordenados = tramos.stream()
                .sorted(Comparator.comparingInt(TramoCancelacion::antelacionMinHoras))
                .toList();
        if (ordenados.isEmpty() || ordenados.getFirst().antelacionMinHoras() != 0) {
            throw new ReglaDominioException("Debe haber un tramo desde 0 horas: toda antelación tiene una penalización");
        }
        if (ordenados.stream().map(TramoCancelacion::antelacionMinHoras).distinct().count() != ordenados.size()) {
            throw new ReglaDominioException("Dos tramos no pueden empezar en la misma antelación");
        }
        this.id = id;
        this.alojamientoId = alojamientoId;
        this.version = version;
        this.tramos = ordenados;
        this.penalizacionNoShow = penalizacionNoShow;
    }

    /**
     * Crea la primera versión de la política del alojamiento (DEC-34).
     *
     * @param minimoTramos {@code ParametrosAlojamiento.minimoTramosCancelacion} (POL-01)
     * @throws ReglaDominioException si tiene menos tramos que el mínimo (POL-01) o los tramos no cubren toda
     *                               antelación sin repetirse (POL-02)
     */
    public static PoliticaCancelacion crear(PoliticaId id, AlojamientoId alojamientoId,
                                            List<TramoCancelacion> tramos, Penalizacion penalizacionNoShow,
                                            int minimoTramos) {
        validarMinimo(tramos, minimoTramos);
        return new PoliticaCancelacion(id, alojamientoId, 1, tramos, penalizacionNoShow);
    }

    /**
     * Crea la versión siguiente sin tocar esta, que queda como historial para las reservas que la congelaron
     * (POL-04 · POL-07). Pasa a ser la vigente por tener la versión más alta (DEC-42).
     *
     * @param minimoTramos {@code ParametrosAlojamiento.minimoTramosCancelacion} (POL-01)
     * @throws ReglaDominioException si tiene menos tramos que el mínimo (POL-01) o los tramos son inválidos (POL-02)
     */
    public PoliticaCancelacion nuevaVersion(PoliticaId nuevoId, List<TramoCancelacion> nuevosTramos,
                                            Penalizacion nuevaPenalizacionNoShow, int minimoTramos) {
        validarMinimo(nuevosTramos, minimoTramos);
        return new PoliticaCancelacion(nuevoId, alojamientoId, version + 1, nuevosTramos, nuevaPenalizacionNoShow);
    }

    /**
     * Penalización por cancelar con esta antelación: la del tramo de mayor antelación mínima que se alcance. Siempre
     * hay exactamente una (POL-02 · TRAM-01).
     *
     * @param horasAntelacion horas completas entre la cancelación y la hora de entrada; negativo cuenta como 0
     */
    public Penalizacion penalizacionPara(long horasAntelacion) {
        long horas = Math.max(0, horasAntelacion);
        return tramos.stream()
                .filter(t -> t.antelacionMinHoras() <= horas)
                .reduce((anterior, siguiente) -> siguiente)
                .orElseThrow()
                .penalizacion();
    }

    // POL-01: el mínimo de la Ficha es configuración del alojamiento
    private static void validarMinimo(List<TramoCancelacion> tramos, int minimoTramos) {
        if (tramos == null || tramos.size() < minimoTramos) {
            throw new ReglaDominioException("La política debe tener al menos " + minimoTramos + " tramos");
        }
    }

    public PoliticaId id() {
        return id;
    }

    public AlojamientoId alojamientoId() {
        return alojamientoId;
    }

    public int version() {
        return version;
    }

    public List<TramoCancelacion> tramos() {
        return tramos;
    }

    public Penalizacion penalizacionNoShow() {
        return penalizacionNoShow;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PoliticaCancelacion otra && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
