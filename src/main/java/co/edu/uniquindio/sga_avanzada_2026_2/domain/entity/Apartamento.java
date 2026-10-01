package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidad Raíz: Agregado Apartamento.
 */
public class Apartamento {

    private final ApartamentoId codigo;
    private final AlojamientoId alojamientoId;
    private final String nombre;
    private Capacidad capacidad;
    private Dormitorio dormitorios;
    private EstadoOperativo estadoOperativo;
    private final List<Imagen> imagenes;
    private final List<Bloqueo> bloqueos;
    private boolean activo;

    private Apartamento(ApartamentoId codigo, AlojamientoId alojamientoId, String nombre,
                        Capacidad capacidad, Dormitorio dormitorios, EstadoOperativo estadoOperativo,
                        List<Imagen> imagenes, List<Bloqueo> bloqueos, boolean activo) {
        this.codigo = codigo;
        this.alojamientoId = alojamientoId;
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.dormitorios = dormitorios;
        this.estadoOperativo = estadoOperativo;
        this.imagenes = new ArrayList<>(imagenes);
        this.bloqueos = new ArrayList<>(bloqueos);
        this.activo = activo;
    }

    public static Apartamento crear(ApartamentoId codigo, AlojamientoId alojamientoId, String nombre,
                                    Capacidad capacidad, Dormitorio dormitorios, EstadoOperativo estadoOperativo,
                                    List<Imagen> imagenes) {
        Objects.requireNonNull(codigo, "El código del apartamento no puede ser nulo");
        Objects.requireNonNull(alojamientoId, "El id del alojamiento no puede ser nulo");
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del apartamento no puede estar vacío");
        }
        Objects.requireNonNull(capacidad, "La capacidad no puede ser nula");
        Objects.requireNonNull(dormitorios, "Los dormitorios no pueden ser nulos");
        Objects.requireNonNull(estadoOperativo, "El estado operativo no puede ser nulo");
        Objects.requireNonNull(imagenes, "La lista de imágenes no puede ser nula");
        if (imagenes.size() < 1 || imagenes.size() > 10) {
            throw new ReglaDominioException("El apartamento debe tener entre 1 y 10 imágenes");
        }

        return new Apartamento(codigo, alojamientoId, nombre.trim(), capacidad, dormitorios,
                estadoOperativo, imagenes, new ArrayList<>(), false);
    }

    public void activar(boolean tarifasCompletas) {
        if (!tarifasCompletas) {
            throw new ReglaDominioException("No se puede activar el apartamento si no tiene tarifas configuradas");
        }
        this.activo = true;
    }

    public void retirarDeVenta(boolean hayReservasActivas) {
        if (hayReservasActivas) {
            throw new ReglaDominioException("No se puede retirar de venta el apartamento porque tiene reservas activas");
        }
        this.activo = false;
    }

    public void cambiarCapacidad(Capacidad nuevaCapacidad) {
        Objects.requireNonNull(nuevaCapacidad, "La nueva capacidad no puede ser nula");
        this.capacidad = nuevaCapacidad;
    }

    public void cambiarEstadoOperativo(EstadoOperativo nuevoEstado) {
        Objects.requireNonNull(nuevoEstado, "El nuevo estado operativo no puede ser nulo");
        if (!this.estadoOperativo.puedePasarA(nuevoEstado)) {
            throw new ReglaDominioException("Transición inválida de " + this.estadoOperativo + " a " + nuevoEstado);
        }
        this.estadoOperativo = nuevoEstado;
    }

    public Bloqueo registrarBloqueo(LocalDate ini, LocalDate fin, String motivo) {
        Bloqueo bloqueo = Bloqueo.crear(BloqueoId.nuevo(), ini, fin, motivo);
        this.bloqueos.add(bloqueo);
        return bloqueo;
    }

    public void levantarBloqueo(BloqueoId id) {
        Objects.requireNonNull(id, "El id del bloqueo a levantar no puede ser nulo");
        Bloqueo bloqueo = this.bloqueos.stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ReglaDominioException("Bloqueo no encontrado en el apartamento"));
        bloqueo.levantar();
    }

    public boolean tieneBloqueoEn(Noche n) {
        Objects.requireNonNull(n, "La noche no puede ser nula");
        return this.bloqueos.stream().anyMatch(b -> b.cubre(n));
    }

    public boolean admite(int totalOcupantes) {
        return this.capacidad.admite(totalOcupantes);
    }

    public boolean estaActivo() {
        return this.activo;
    }

    public ApartamentoId getCodigo() {
        return codigo;
    }

    public ApartamentoId getId() {
        return codigo;
    }

    public ApartamentoId getIdentificacion() {
        return codigo;
    }

    public AlojamientoId getAlojamientoId() {
        return alojamientoId;
    }

    public String getNombre() {
        return nombre;
    }

    public Capacidad getCapacidad() {
        return capacidad;
    }

    public Dormitorio getDormitorios() {
        return dormitorios;
    }

    public EstadoOperativo getEstadoOperativo() {
        return estadoOperativo;
    }

    public List<Imagen> getImagenes() {
        return Collections.unmodifiableList(imagenes);
    }

    public List<Bloqueo> getBloqueos() {
        return Collections.unmodifiableList(bloqueos);
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Apartamento that = (Apartamento) o;
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
