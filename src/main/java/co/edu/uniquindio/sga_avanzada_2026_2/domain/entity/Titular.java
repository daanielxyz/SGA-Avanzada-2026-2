package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TitularId;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Entidad Raíz: Agregado Titular (cliente principal responsable de la reserva).
 */
public class Titular {

    private static final Pattern PATRON_CORREO = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final TitularId id;
    private String nombre;
    private Documento documento;
    private String correo;
    private String telefono;

    private Titular(TitularId id, String nombre, Documento documento, String correo, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.documento = documento;
        this.correo = correo;
        this.telefono = telefono;
    }

    public static Titular crear(TitularId id, String nombre, Documento documento, String correo, String telefono) {
        Objects.requireNonNull(id, "El id del titular no puede ser nulo");
        validarCampos(nombre, documento, correo, telefono);
        return new Titular(id, nombre.trim(), documento, correo.trim().toLowerCase(), telefono.trim());
    }

    public void actualizarDatos(String nuevoNombre, Documento nuevoDocumento, String nuevoCorreo, String nuevoTelefono) {
        validarCampos(nuevoNombre, nuevoDocumento, nuevoCorreo, nuevoTelefono);
        this.nombre = nuevoNombre.trim();
        this.documento = nuevoDocumento;
        this.correo = nuevoCorreo.trim().toLowerCase();
        this.telefono = nuevoTelefono.trim();
    }

    private static void validarCampos(String nombre, Documento documento, String correo, String telefono) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del titular no puede estar vacío");
        }
        Objects.requireNonNull(documento, "El documento del titular no puede ser nulo");
        if (correo == null || correo.isBlank() || !PATRON_CORREO.matcher(correo.trim()).matches()) {
            throw new ReglaDominioException("El formato del correo electrónico es inválido: " + correo);
        }
        if (telefono == null || telefono.isBlank()) {
            throw new ReglaDominioException("El teléfono del titular no puede estar vacío");
        }
    }

    public TitularId getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Documento getDocumento() {
        return documento;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Titular titular = (Titular) o;
        return Objects.equals(id, titular.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
