package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MotivoRechazo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Raíz del agregado NotaReserva: lo que un turno le deja dicho al siguiente sobre una reserva («pagaron la mitad»,
 * «falta la cédula», «pidieron cobijas»; BL-08 · DEC-105). Es del personal, nunca del huésped. El texto no se edita
 * ni se borra: cuando se resuelve, se marca como atendida y queda quién y cuándo. Referencia la reserva solo por su
 * código, así escribir una nota no compite con los cambios de la reserva.
 */
public class NotaReserva {

    /** Lo que cabe en una nota; lo largo va en varias. */
    public static final int LARGO_MAXIMO = 500;

    private final NotaReservaId id;
    private final ReservaId reservaId;
    private final String texto;
    private final UsuarioId autor;
    private final LocalDateTime escritaEn;
    private AtencionNota atencion; // nula mientras está pendiente

    // BL-08 · DEC-105
    public NotaReserva(NotaReservaId id, ReservaId reservaId, String texto, UsuarioId autor,
                       LocalDateTime escritaEn, AtencionNota atencion) {
        if (id == null || reservaId == null || autor == null || escritaEn == null) {
            throw new ReglaDominioException("La nota requiere id, reserva, autor y fecha");
        }
        if (texto == null || texto.isBlank()) {
            throw new ReglaDominioException("El texto de la nota es obligatorio");
        }
        if (texto.strip().length() > LARGO_MAXIMO) {
            throw new ReglaDominioException("La nota admite máximo " + LARGO_MAXIMO + " caracteres");
        }
        if (atencion != null && atencion.fechaHora().isBefore(escritaEn)) {
            throw new ReglaDominioException("Una nota no se atiende antes de escribirse");
        }
        this.id = id;
        this.reservaId = reservaId;
        this.texto = texto.strip();
        this.autor = autor;
        this.escritaEn = escritaEn;
        this.atencion = atencion;
    }

    /**
     * Escribe una nota pendiente en la reserva (BL-08). Solo en una reserva activa: una terminada ya no tiene turno
     * siguiente que enterar (DEC-105).
     *
     * @param ahora fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si la reserva no está activa (RN-08) o el texto falta o es muy largo
     */
    public static NotaReserva escribir(NotaReservaId id, Reserva reserva, String texto, UsuarioId autor,
                                       LocalDateTime ahora) {
        Objects.requireNonNull(reserva, "reserva");
        exigirActiva(reserva, "Las notas solo se escriben en una reserva activa");
        return new NotaReserva(id, reserva.codigo(), texto, autor, ahora, null);
    }

    /**
     * Marca la nota como atendida, con quién y cuándo (BL-08 · DEC-105). Deja de salir como pendiente.
     *
     * @param reserva la reserva de la nota, para saber si sigue activa
     * @param ahora   fecha y hora actuales en Colombia, inyectadas
     * @throws ReglaDominioException si ya estaba atendida (pudo hacerlo otro turno), si la reserva no está activa o
     *                               si la reserva no es la de la nota
     */
    public void atender(Reserva reserva, UsuarioId autorAtencion, LocalDateTime ahora) {
        Objects.requireNonNull(reserva, "reserva");
        if (!reserva.codigo().equals(reservaId)) {
            throw new ReglaDominioException(
                    "Esa nota no es de la reserva " + reserva.codigo().valor());
        }
        if (atendida()) {
            throw new ReglaDominioException(MotivoRechazo.YA_ATENDIDA, "Esa nota ya fue atendida");
        }
        exigirActiva(reserva, "Las notas solo se atienden en una reserva activa");
        if (ahora == null || ahora.isBefore(escritaEn)) {
            throw new ReglaDominioException("Una nota no se atiende antes de escribirse");
        }
        atencion = new AtencionNota(autorAtencion, ahora);
    }

    private static void exigirActiva(Reserva reserva, String mensaje) {
        if (!reserva.estaActiva()) {
            throw new ReglaDominioException(MotivoRechazo.ESTADO_NO_PERMITIDO,
                    mensaje + ": la reserva está " + reserva.estado().texto());
        }
    }

    public boolean atendida() {
        return atencion != null;
    }

    public NotaReservaId id() {
        return id;
    }

    public ReservaId reservaId() {
        return reservaId;
    }

    public String texto() {
        return texto;
    }

    public UsuarioId autor() {
        return autor;
    }

    public LocalDateTime escritaEn() {
        return escritaEn;
    }

    /** Quién la atendió y cuándo; nula si sigue pendiente. */
    public AtencionNota atencion() {
        return atencion;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NotaReserva otra && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
