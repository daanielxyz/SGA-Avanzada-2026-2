package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.ClavesReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConsultarReservaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.AccesoDenegadoException;
import org.springframework.stereotype.Component;

/**
 * Quién puede pagar o liberar una reserva del portal sin ser personal (DEC-93): quien trae su clave, la que recibió
 * al reservar, o el huésped dueño con su sesión. Sin ninguna de las dos, el código solo, que es consecutivo, no basta.
 * Una clave equivocada responde igual exista o no la reserva, para no revelar cuáles existen.
 */
@Component
class AccesoReservaPortal {

    private final ClavesReserva claves;
    private final UsuarioActual usuario;
    private final ConsultarReservaUseCase consultarReserva;

    AccesoReservaPortal(ClavesReserva claves, UsuarioActual usuario, ConsultarReservaUseCase consultarReserva) {
        this.claves = claves;
        this.usuario = usuario;
        this.consultarReserva = consultarReserva;
    }

    /**
     * @throws AccesoDenegadoException si la clave no es la de la reserva o, sin clave, no hay sesión o la reserva es de
     *                                 otro huésped
     */
    void exigirClaveOSesion(String codigo, String clave) {
        if (clave == null && usuario.conSesion()) {
            usuario.exigirAccesoA(consultarReserva.ejecutar(codigo).titularId()); // USU-04
            return;
        }
        exigirClave(codigo, clave);
    }

    /** @throws AccesoDenegadoException si falta la clave o no es la de la reserva */
    void exigirClave(String codigo, String clave) {
        if (clave == null || !claves.corresponde(codigo, clave)) {
            throw new AccesoDenegadoException("Hace falta la clave de la reserva que se recibió al reservar");
        }
    }
}
