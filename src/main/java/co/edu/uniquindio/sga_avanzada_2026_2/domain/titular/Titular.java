package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Raíz del agregado Titular: responsable de la reserva y del pago de su folio (TIT-02). Puede existir sin usuario
 * del sistema (TIT-03) y lo es de varias reservas (TIT-05). Los datos del TRA son de cada ocupante, no de aquí
 * (DEC-49).
 */
public class Titular {

    private final TitularId id;
    private final AlojamientoId alojamientoId; // DEC-26
    private String nombre;
    private final Documento documento; // lo identifica entre los ocupantes (TIT-01); no cambia
    private Correo correo;             // opcional si hay teléfono
    private String telefono;           // opcional si hay correo

    // TIT-01 · DEC-49
    public Titular(TitularId id, AlojamientoId alojamientoId, String nombre, Documento documento, Correo correo,
                   String telefono) {
        if (id == null || alojamientoId == null || documento == null) {
            throw new ReglaDominioException("El titular requiere id, alojamiento y documento");
        }
        this.id = id;
        this.alojamientoId = alojamientoId;
        this.documento = documento;
        asignarDatos(nombre, correo, telefono);
    }

    /**
     * Actualiza nombre y datos de contacto. El documento no cambia: es lo que lo reconoce entre los ocupantes de sus
     * reservas (TIT-01 · TIT-05).
     *
     * @throws ReglaDominioException si falta el nombre, no queda ningún contacto o el teléfono es inválido (DEC-49)
     */
    public void actualizarDatos(String nuevoNombre, Correo nuevoCorreo, String nuevoTelefono) {
        asignarDatos(nuevoNombre, nuevoCorreo, nuevoTelefono);
    }

    // DEC-49: nombre y documento obligatorios, y al menos un contacto (correo o teléfono)
    private void asignarDatos(String nuevoNombre, Correo nuevoCorreo, String nuevoTelefono) {
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new ReglaDominioException("El nombre del titular es obligatorio");
        }
        String telefonoLimpio = nuevoTelefono == null || nuevoTelefono.isBlank() ? null : nuevoTelefono.trim();
        if (nuevoCorreo == null && telefonoLimpio == null) {
            throw new ReglaDominioException("El titular necesita al menos un contacto: correo o teléfono");
        }
        if (telefonoLimpio != null && !esTelefonoValido(telefonoLimpio)) {
            throw new ReglaDominioException("Teléfono inválido: " + telefonoLimpio);
        }
        nombre = nuevoNombre.trim();
        correo = nuevoCorreo;
        telefono = telefonoLimpio;
    }

    // Dígitos con separadores comunes y un «+» inicial opcional; entre 7 y 15 dígitos (largo de un número E.164)
    private static boolean esTelefonoValido(String telefono) {
        long digitos = telefono.chars().filter(Character::isDigit).count();
        boolean caracteresValidos = telefono.chars()
                .allMatch(c -> Character.isDigit(c) || c == ' ' || c == '-' || c == '(' || c == ')' || c == '+');
        return caracteresValidos && telefono.lastIndexOf('+') <= 0 && digitos >= 7 && digitos <= 15;
    }

    public TitularId id() {
        return id;
    }

    public AlojamientoId alojamientoId() {
        return alojamientoId;
    }

    public String nombre() {
        return nombre;
    }

    public Documento documento() {
        return documento;
    }

    public Correo correo() {
        return correo;
    }

    public String telefono() {
        return telefono;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Titular otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
