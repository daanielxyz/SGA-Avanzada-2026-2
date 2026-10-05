package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.util.Objects;

/**
 * Entidad interna de Alojamiento (SERV-01). Su valor se congela en el Cargo al cobrarse (SERV-02); nunca se
 * borra, se desactiva (SERV-04). Solo cambia a través de su Alojamiento (DEC-23).
 */
public class ServicioAdicional {

    private final ServicioAdicionalId id;
    private final String nombre;
    private final boolean generaCargo;
    private Dinero valor; // ≥ 0 (Dinero nunca es negativo, DEC-20)
    private boolean activo;

    // SERV-01
    public ServicioAdicional(ServicioAdicionalId id, String nombre, boolean generaCargo, Dinero valor,
                             boolean activo) {
        if (id == null || valor == null) {
            throw new ReglaDominioException("El servicio adicional requiere id y valor");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del servicio adicional es obligatorio");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.generaCargo = generaCargo;
        this.valor = valor;
        this.activo = activo;
    }

    /**
     * Reemplaza el valor. Los cargos ya registrados conservan el valor con que se cobraron (SERV-02).
     */
    void cambiarValor(Dinero nuevo) {
        valor = Objects.requireNonNull(nuevo, "nuevo");
    }

    /**
     * Eliminación lógica: deja de ofrecerse y conserva el histórico (SERV-04).
     *
     * @throws ReglaDominioException si ya estaba inactivo
     */
    void desactivar() {
        if (!activo) {
            throw new ReglaDominioException("El servicio " + id.valor() + " ya está inactivo");
        }
        activo = false;
    }

    public ServicioAdicionalId id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public boolean generaCargo() {
        return generaCargo;
    }

    public Dinero valor() {
        return valor;
    }

    public boolean activo() {
        return activo;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ServicioAdicional otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
