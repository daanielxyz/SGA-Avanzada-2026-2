package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.FormatoTexto;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MotivoRechazo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Raíz del agregado Titular: responsable de la reserva y del pago de su folio (TIT-02) y lo es de varias reservas
 * (TIT-05). Con correo, el titular es además la cuenta del huésped (DEC-68), que vive fuera del dominio; sin correo
 * (p. ej. un canal externo que no lo envía) no tiene cuenta (TIT-03). Los datos del TRA son de cada ocupante. Cuando
 * recepción valida su identidad en persona, su nombre ya solo lo cambia el hotel (DEC-98).
 */
public class Titular {

    private final TitularId id;
    private final AlojamientoId alojamientoId; // DEC-26
    private String nombres;
    private String primerApellido;     // opcional: los titulares de antes lo traen dentro de nombres (DEC-98)
    private String segundoApellido;    // opcional: no todos tienen
    private Documento documento;       // lo reconoce entre sus reservas (TIT-05); se corrige si se escribió mal
    private Correo correo;             // opcional si hay teléfono; con él, el titular tiene cuenta (DEC-68)
    private String telefono;           // opcional si hay correo
    private ValidacionIdentidad validacion; // null hasta que recepción la valida en persona (DEC-98)

    // DEC-49: con el nombre completo en un solo texto
    public Titular(TitularId id, AlojamientoId alojamientoId, String nombre, Documento documento, Correo correo,
                   String telefono) {
        this(id, alojamientoId, nombre, null, null, documento, correo, telefono, null);
    }

    // DEC-49 · DEC-98
    public Titular(TitularId id, AlojamientoId alojamientoId, String nombres, String primerApellido,
                   String segundoApellido, Documento documento, Correo correo, String telefono,
                   ValidacionIdentidad validacion) {
        if (id == null || alojamientoId == null || documento == null) {
            throw new ReglaDominioException("El titular requiere id, alojamiento y documento");
        }
        validarIdentifica(documento);
        this.id = id;
        this.alojamientoId = alojamientoId;
        this.documento = documento;
        this.validacion = validacion;
        asignarDatos(nombres, primerApellido, segundoApellido, correo, telefono);
    }

    /**
     * Recepción o el administrador corrigen nombre y contacto, también de un titular validado: es el hotel. El
     * documento no cambia aquí: es lo que lo reconoce entre sus reservas (TIT-05); si se escribió mal, se corrige con
     * {@link #corregirDocumento(Documento)}.
     *
     * @throws ReglaDominioException si falta el nombre, no queda ningún contacto o el teléfono es inválido (DEC-49)
     */
    public void actualizarDatos(String nuevosNombres, String nuevoPrimerApellido, String nuevoSegundoApellido,
                                Correo nuevoCorreo, String nuevoTelefono) {
        asignarDatos(nuevosNombres, nuevoPrimerApellido, nuevoSegundoApellido, nuevoCorreo, nuevoTelefono);
    }

    /** Como {@link #actualizarDatos(String, String, String, Correo, String)}, con el nombre en un solo texto. */
    public void actualizarDatos(String nuevoNombre, Correo nuevoCorreo, String nuevoTelefono) {
        actualizarDatos(nuevoNombre, null, null, nuevoCorreo, nuevoTelefono);
    }

    /**
     * El huésped cambia su nombre y su teléfono desde su cuenta (DEC-68). Con la identidad validada, el nombre ya no
     * lo cambia él sino recepción; el teléfono sí (DEC-98). Separar los apellidos sin cambiar el nombre sí se puede
     * siempre: así se arreglan las cuentas de antes, que lo guardaron todo junto (DEC-109).
     *
     * @throws ReglaDominioException si la identidad está validada y el nombre cambia, falta el nombre o el teléfono es
     *                               inválido (DEC-49)
     */
    public void actualizarDesdeCuenta(String nuevosNombres, String nuevoPrimerApellido, String nuevoSegundoApellido,
                                      String nuevoTelefono) {
        boolean cambiaNombre = !nombreCompleto(nuevosNombres, nuevoPrimerApellido, nuevoSegundoApellido)
                .equalsIgnoreCase(nombre());
        if (estaValidado() && cambiaNombre) {
            throw new ReglaDominioException(MotivoRechazo.IDENTIDAD_VALIDADA, "El hotel ya validó tu identidad: "
                    + "para cambiar tu nombre, pídelo en recepción");
        }
        // DEC-109: con el mismo nombre todo junto se conserva como estaba, para no borrar apellidos ya separados
        boolean conservaComoEstaba = !cambiaNombre && limpio(nuevoPrimerApellido) == null;
        if (conservaComoEstaba) {
            asignarDatos(nombres, primerApellido, segundoApellido, correo, nuevoTelefono);
            return;
        }
        asignarDatos(nuevosNombres, nuevoPrimerApellido, nuevoSegundoApellido, correo, nuevoTelefono);
    }

    /**
     * Recepción confirma en persona, con el documento, que el titular es quien dice ser (DEC-98). Desde entonces su
     * nombre solo lo cambia el hotel. No se deshace.
     *
     * @param autor quien validó, del usuario de la sesión
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si ya estaba validada
     */
    public void validarIdentidad(UsuarioId autor, LocalDateTime ahora) {
        if (estaValidado()) {
            throw new ReglaDominioException(MotivoRechazo.IDENTIDAD_VALIDADA,
                    "La identidad de " + nombre() + " ya está validada desde el "
                    + FormatoTexto.fechaHora(validacion.fechaHora()));
        }
        validacion = new ValidacionIdentidad(autor, ahora);
    }

    /**
     * Completa el contacto que le falte con lo que llegó en una nueva reserva, sin reemplazar el que ya tiene: quien
     * conoce un documento no puede quedarse con la cuenta de otro cambiando su correo o su teléfono (DEC-69).
     *
     * @throws ReglaDominioException si el teléfono que completa es inválido
     */
    public void completarContacto(Correo correoRecibido, String telefonoRecibido) {
        asignarDatos(nombres, primerApellido, segundoApellido, correo == null ? correoRecibido : correo,
                telefono == null ? telefonoRecibido : telefono);
    }

    /**
     * Cambia el correo, que es también el de su cuenta. Solo se llama después de verificar el PIN del cambio de
     * correo o desde recepción (DEC-68 · DEC-69). Vale también con la identidad validada (DEC-98).
     *
     * @throws ReglaDominioException si el correo nuevo falta
     */
    public void cambiarCorreo(Correo nuevoCorreo) {
        if (nuevoCorreo == null) {
            throw new ReglaDominioException("El correo nuevo es obligatorio");
        }
        asignarDatos(nombres, primerApellido, segundoApellido, nuevoCorreo, telefono);
    }

    /**
     * Corrige su documento cuando se escribió mal (#14 · DEC-120). Sigue siendo el mismo titular, con sus reservas,
     * su validación y su cuenta; desde ahora el huésped entra con el número corregido (DEC-68). Que el documento
     * nuevo no sea de otro titular del alojamiento (TIT-05) lo revisa {@code CorreccionDocumentoDomainService}.
     *
     * @throws ReglaDominioException si el documento es el mismo o es un registro civil (TIT-01)
     */
    public void corregirDocumento(Documento nuevo) {
        if (nuevo == null) {
            throw new ReglaDominioException("El documento nuevo es obligatorio");
        }
        if (nuevo.equals(documento)) {
            throw new ReglaDominioException("El documento de " + nombre() + " ya es ese");
        }
        validarIdentifica(nuevo);
        documento = nuevo;
    }

    // TIT-01: el titular es responsable de la reserva; un registro civil es de un niño
    private static void validarIdentifica(Documento documento) {
        if (!documento.tipo().identificaTitular()) {
            throw new ReglaDominioException(MotivoRechazo.TITULAR_INVALIDO,
                    "El titular no se identifica con registro civil: ese documento es de un niño");
        }
    }

    // DEC-49: nombre y documento obligatorios, y al menos un contacto (correo o teléfono)
    private void asignarDatos(String nuevosNombres, String nuevoPrimerApellido, String nuevoSegundoApellido,
                              Correo nuevoCorreo, String nuevoTelefono) {
        if (limpio(nuevosNombres) == null) {
            throw new ReglaDominioException("El nombre del titular es obligatorio");
        }
        String telefonoLimpio = limpio(nuevoTelefono);
        if (nuevoCorreo == null && telefonoLimpio == null) {
            throw new ReglaDominioException("El titular necesita al menos un contacto: correo o teléfono");
        }
        if (telefonoLimpio != null && !esTelefonoValido(telefonoLimpio)) {
            throw new ReglaDominioException("Teléfono inválido: " + telefonoLimpio);
        }
        nombres = limpio(nuevosNombres);
        primerApellido = limpio(nuevoPrimerApellido);
        segundoApellido = limpio(nuevoSegundoApellido);
        correo = nuevoCorreo;
        telefono = telefonoLimpio;
    }

    private static String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String nombreCompleto(String nombres, String primerApellido, String segundoApellido) {
        return Stream.of(nombres, primerApellido, segundoApellido)
                .map(Titular::limpio)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    // Dígitos con separadores comunes y un «+» inicial opcional; entre 7 y 15 dígitos (largo de un número E.164)
    private static boolean esTelefonoValido(String telefono) {
        long digitos = telefono.chars().filter(Character::isDigit).count();
        boolean caracteresValidos = telefono.chars()
                .allMatch(c -> Character.isDigit(c) || c == ' ' || c == '-' || c == '(' || c == ')' || c == '+');
        return caracteresValidos && telefono.lastIndexOf('+') <= 0 && digitos >= 7 && digitos <= 15;
    }

    public boolean estaValidado() {
        return validacion != null;
    }

    public TitularId id() {
        return id;
    }

    public AlojamientoId alojamientoId() {
        return alojamientoId;
    }

    /** Nombres y apellidos juntos, como se muestran. */
    public String nombre() {
        return nombreCompleto(nombres, primerApellido, segundoApellido);
    }

    public String nombres() {
        return nombres;
    }

    public String primerApellido() {
        return primerApellido;
    }

    public String segundoApellido() {
        return segundoApellido;
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

    /** {@code null} si recepción no la ha validado. */
    public ValidacionIdentidad validacion() {
        return validacion;
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
