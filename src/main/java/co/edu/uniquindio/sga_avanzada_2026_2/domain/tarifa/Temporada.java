package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad interna de CalendarioTemporadas. Una específica tiene rango [fechaInicio, fechaFin] inclusivo en ambos
 * extremos (TEM-01); la base no tiene rango: cubre lo que ninguna otra cubre (TEM-09). Nunca se borra, se
 * desactiva (TEM-06). Solo cambia a través de su calendario (DEC-23).
 */
public class Temporada {

    private final TemporadaId id;
    private final String nombre;
    private LocalDate fechaInicio; // null en la base
    private LocalDate fechaFin;    // inclusiva; null en la base
    private final boolean esBase;
    private final int estanciaMinimaNoches; // 0 = sin mínimo; la base no declara (TEM-07)
    private boolean activa;

    // TEM-01 · TEM-07 · TEM-09 · TEM-10
    public Temporada(TemporadaId id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, boolean esBase,
                     int estanciaMinimaNoches, boolean activa) {
        if (id == null) {
            throw new ReglaDominioException("La temporada requiere id");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre de la temporada es obligatorio");
        }
        if (estanciaMinimaNoches < 0) {
            throw new ReglaDominioException("La estancia mínima no puede ser negativa");
        }
        if (esBase) {
            if (fechaInicio != null || fechaFin != null || estanciaMinimaNoches != 0 || !activa) {
                throw new ReglaDominioException(
                        "La temporada base no tiene rango ni estancia mínima y siempre está activa");
            }
        } else {
            validarRango(fechaInicio, fechaFin);
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.esBase = esBase;
        this.estanciaMinimaNoches = estanciaMinimaNoches;
        this.activa = activa;
    }

    /**
     * Indica si esta temporada específica y activa contiene la noche, con ambos extremos incluidos (TEM-01).
     * La base siempre responde {@code false}: su cobertura la resuelve {@link CalendarioTemporadas#temporadaDe}.
     */
    public boolean cubre(Noche noche) {
        Objects.requireNonNull(noche, "noche");
        LocalDate fecha = noche.fecha();
        return !esBase && activa && !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }

    /**
     * Dos temporadas específicas activas se solapan si comparten al menos una fecha (rangos inclusivos). La base no
     * compite en el solapamiento (TEM-02 · TEM-09).
     */
    public boolean seSolapaCon(Temporada otra) {
        Objects.requireNonNull(otra, "otra");
        if (esBase || otra.esBase || !activa || !otra.activa) {
            return false;
        }
        return !fechaInicio.isAfter(otra.fechaFin) && !otra.fechaInicio.isAfter(fechaFin);
    }

    /**
     * Reemplaza el rango. Las reservas ya creadas no cambian porque su valor está congelado (TEM-05).
     */
    void cambiarFechas(LocalDate inicio, LocalDate fin) {
        if (esBase) {
            throw new ReglaDominioException("La temporada base no tiene rango de fechas");
        }
        validarRango(inicio, fin);
        fechaInicio = inicio;
        fechaFin = fin;
    }

    /**
     * Eliminación lógica: sus fechas pasan a la base (TEM-06 · TEM-11).
     *
     * @throws ReglaDominioException si es la base (TEM-10) o ya estaba inactiva
     */
    void desactivar() {
        if (esBase) {
            throw new ReglaDominioException("La temporada base no puede desactivarse");
        }
        if (!activa) {
            throw new ReglaDominioException("La temporada " + id.valor() + " ya está inactiva");
        }
        activa = false;
    }

    // TEM-01
    private static void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new ReglaDominioException("Una temporada específica requiere fecha de inicio y de fin");
        }
        if (fin.isBefore(inicio)) {
            throw new ReglaDominioException("El fin de la temporada no puede ser anterior al inicio");
        }
    }

    public TemporadaId id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public LocalDate fechaInicio() {
        return fechaInicio;
    }

    public LocalDate fechaFin() {
        return fechaFin;
    }

    public boolean esBase() {
        return esBase;
    }

    public int estanciaMinimaNoches() {
        return estanciaMinimaNoches;
    }

    public boolean activa() {
        return activa;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Temporada otra && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
