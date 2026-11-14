package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Entidad interna de Reserva. Se guarda la fecha de nacimiento, nunca la edad (OCU-01). Todos cuentan para la
 * capacidad (CAP-03); solo los facturables pagan (OCU-09). Que la fecha de nacimiento no sea futura (OCU-12) y que
 * no esté repetido (OCU-05) lo valida la Reserva, porque depende de la fecha actual y del resto del grupo (DEC-06).
 * Los datos del TRA pueden faltar hasta la salida (BL-07); completarlos crea un ocupante nuevo con el mismo id. Uno
 * del grupo es el huésped principal del TRA (Resolución 700 de 2021): se aloja y es mayor de edad (DEC-97).
 */
public class Ocupante {

    /** Mayoría de edad en Colombia (Ley 27 de 1977); la exige el huésped principal (DEC-97). */
    public static final int MAYORIA_DE_EDAD = 18;

    private final OcupanteId id;
    private final String nombres;
    private final String primerApellido;  // opcional hasta la salida (BL-07)
    private final String segundoApellido; // opcional: no todos tienen
    private final LocalDate fechaNacimiento;
    private final Documento documento;   // opcional al reservar; el TRA lo exige antes de la salida (BL-07)
    private final String nacionalidad;   // opcional (OCU-07); el TRA la exige (BL-07)
    private final DatosViaje viaje;
    private final boolean principal;     // huésped principal del TRA; lo marca la Reserva (DEC-97)

    // OCU-07
    public Ocupante(OcupanteId id, String nombre, LocalDate fechaNacimiento, Documento documento,
                    String nacionalidad) {
        this(id, nombre, null, null, fechaNacimiento, documento, nacionalidad, DatosViaje.VACIOS);
    }

    // OCU-07 · BL-07
    public Ocupante(OcupanteId id, String nombres, String primerApellido, String segundoApellido,
                    LocalDate fechaNacimiento, Documento documento, String nacionalidad, DatosViaje viaje) {
        this(id, nombres, primerApellido, segundoApellido, fechaNacimiento, documento, nacionalidad, viaje, false);
    }

    // OCU-07 · BL-07 · DEC-97
    public Ocupante(OcupanteId id, String nombres, String primerApellido, String segundoApellido,
                    LocalDate fechaNacimiento, Documento documento, String nacionalidad, DatosViaje viaje,
                    boolean principal) {
        if (id == null || fechaNacimiento == null) {
            throw new ReglaDominioException("El ocupante requiere id y fecha de nacimiento");
        }
        if (nombres == null || nombres.isBlank()) {
            throw new ReglaDominioException("El nombre del ocupante es obligatorio");
        }
        this.id = id;
        this.nombres = nombres.trim();
        this.primerApellido = DatosViaje.limpio(primerApellido);
        this.segundoApellido = DatosViaje.limpio(segundoApellido);
        this.fechaNacimiento = fechaNacimiento;
        this.documento = documento;
        this.nacionalidad = DatosViaje.limpio(nacionalidad);
        this.viaje = viaje == null ? DatosViaje.VACIOS : viaje;
        this.principal = principal;
    }

    /** DEC-97: el huésped principal debe ser mayor de edad a la fecha de entrada. */
    public boolean esMayorDeEdadA(LocalDate fechaEntrada) {
        return edadA(fechaEntrada) >= MAYORIA_DE_EDAD;
    }

    /** El mismo ocupante, marcado o no como huésped principal (DEC-97). */
    Ocupante comoPrincipal(boolean esPrincipal) {
        return new Ocupante(id, nombres, primerApellido, segundoApellido, fechaNacimiento, documento, nacionalidad,
                viaje, esPrincipal);
    }

    /**
     * Años cumplidos a la fecha dada; se calcula, nunca se guarda (OCU-13 · OCU-01).
     */
    public int edadA(LocalDate fecha) {
        Objects.requireNonNull(fecha, "fecha");
        return Period.between(fechaNacimiento, fecha).getYears();
    }

    /**
     * Es facturable si a la fecha de entrada alcanza o supera el umbral configurado. Cumplir años durante la
     * estancia no lo cambia (RN-06 · OCU-03 · OCU-08 · OCU-11).
     *
     * @param fechaEntrada fecha de entrada de la estancia, no la de cada noche
     * @param umbral       {@code ParametrosAlojamiento.umbralEdadFacturable}
     */
    public boolean esFacturableA(LocalDate fechaEntrada, int umbral) {
        return edadA(fechaEntrada) >= umbral;
    }

    /**
     * Detecta si otro registro es la misma persona: por documento si ambos lo tienen; si no, por nombre completo (sin
     * distinguir mayúsculas) y fecha de nacimiento (OCU-05).
     *
     * @return {@code true} si no pueden estar los dos en la misma reserva
     */
    public boolean esDuplicadoDe(Ocupante otro) {
        Objects.requireNonNull(otro, "otro");
        if (documento != null && otro.documento != null) {
            return documento.equals(otro.documento);
        }
        return nombre().equalsIgnoreCase(otro.nombre()) && fechaNacimiento.equals(otro.fechaNacimiento);
    }

    /**
     * Completa los datos del TRA con los que llegan; el que no llega se conserva. La fecha de nacimiento no cambia,
     * porque decide quién paga (RN-06), y un documento ya registrado tampoco: si está mal escrito, se corrige con
     * {@code Reserva.corregirDocumento} (BL-07 · DEC-120).
     *
     * @return el mismo ocupante, con sus datos completados
     * @throws ReglaDominioException si llega un documento distinto del que ya tenía
     */
    Ocupante completar(String nuevosNombres, String nuevoPrimerApellido, String nuevoSegundoApellido,
                       Documento nuevoDocumento, String nuevaNacionalidad, DatosViaje nuevoViaje) {
        if (documento != null && nuevoDocumento != null && !documento.equals(nuevoDocumento)) {
            throw new ReglaDominioException("El documento de " + nombre() + " ya está registrado; si está mal "
                    + "escrito, usa «Corregir documento»");
        }
        return new Ocupante(id,
                DatosViaje.limpio(nuevosNombres) != null ? nuevosNombres : nombres,
                DatosViaje.limpio(nuevoPrimerApellido) != null ? nuevoPrimerApellido : primerApellido,
                DatosViaje.limpio(nuevoSegundoApellido) != null ? nuevoSegundoApellido : segundoApellido,
                fechaNacimiento,
                documento != null ? documento : nuevoDocumento,
                DatosViaje.limpio(nuevaNacionalidad) != null ? nuevaNacionalidad : nacionalidad,
                viaje.completarCon(nuevoViaje), principal);
    }

    /** El mismo ocupante con otro documento; todo lo demás, también los datos del TRA, se conserva (DEC-120). */
    Ocupante conDocumento(Documento nuevo) {
        return new Ocupante(id, nombres, primerApellido, segundoApellido, fechaNacimiento, nuevo, nacionalidad, viaje,
                principal);
    }

    /**
     * Al modificar la reserva el grupo llega de nuevo: si este ocupante es la misma persona que uno anterior, conserva
     * los datos del TRA que ya se habían tomado y que ahora no llegan (BL-07 · OCU-05).
     */
    Ocupante conservandoDatosDe(Ocupante anterior) {
        return new Ocupante(id, nombres,
                primerApellido != null ? primerApellido : anterior.primerApellido,
                segundoApellido != null ? segundoApellido : anterior.segundoApellido,
                fechaNacimiento,
                documento != null ? documento : anterior.documento,
                nacionalidad != null ? nacionalidad : anterior.nacionalidad,
                anterior.viaje.completarCon(viaje), principal);
    }

    /** Los datos del TRA que todavía le faltan, en el orden del formulario (BL-07). */
    public List<DatoTra> datosTraFaltantes() {
        List<DatoTra> faltan = new ArrayList<>();
        agregarSiFalta(faltan, DatoTra.DOCUMENTO, documento);
        agregarSiFalta(faltan, DatoTra.PRIMER_APELLIDO, primerApellido);
        agregarSiFalta(faltan, DatoTra.NACIONALIDAD, nacionalidad);
        agregarSiFalta(faltan, DatoTra.GENERO, viaje.genero());
        agregarSiFalta(faltan, DatoTra.PAIS_RESIDENCIA, viaje.paisResidencia());
        agregarSiFalta(faltan, DatoTra.CIUDAD_RESIDENCIA, viaje.ciudadResidencia());
        agregarSiFalta(faltan, DatoTra.CIUDAD_PROCEDENCIA, viaje.ciudadProcedencia());
        agregarSiFalta(faltan, DatoTra.CIUDAD_DESTINO, viaje.ciudadDestino());
        agregarSiFalta(faltan, DatoTra.MOTIVO_VIAJE, viaje.motivoViaje());
        return List.copyOf(faltan);
    }

    private static void agregarSiFalta(List<DatoTra> faltan, DatoTra dato, Object valor) {
        if (valor == null) {
            faltan.add(dato);
        }
    }

    public OcupanteId id() {
        return id;
    }

    /** Nombres y apellidos juntos, como se muestran. */
    public String nombre() {
        return Stream.of(nombres, primerApellido, segundoApellido)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
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

    public LocalDate fechaNacimiento() {
        return fechaNacimiento;
    }

    public Documento documento() {
        return documento;
    }

    public String nacionalidad() {
        return nacionalidad;
    }

    public DatosViaje viaje() {
        return viaje;
    }

    public boolean principal() {
        return principal;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Ocupante otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
