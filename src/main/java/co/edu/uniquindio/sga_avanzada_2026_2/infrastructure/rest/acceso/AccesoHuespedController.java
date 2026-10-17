package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.AccesoHuesped;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acceso del huésped, que entra a la cuenta de su titular con un PIN (DEC-68 · DEC-69). Todo es público: el PIN es
 * lo que prueba quién es. Las solicitudes de PIN responden lo mismo exista o no la cuenta.
 */
@RestController
@RequestMapping("/api/auth/huesped")
public class AccesoHuespedController {

    private final AccesoHuesped acceso;

    public AccesoHuespedController(AccesoHuesped acceso) {
        this.acceso = acceso;
    }

    @PostMapping("/pin")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MensajeResponse solicitarPin(@Valid @RequestBody SolicitarPinRequest solicitud) {
        acceso.solicitarPin(solicitud.correo(), solicitud.numeroDocumento());
        return new MensajeResponse("Si los datos corresponden a una cuenta, le enviamos un PIN al correo. Vence en 15"
                + " minutos.");
    }

    @PostMapping("/login")
    public SesionResponse iniciarSesion(@Valid @RequestBody IniciarSesionHuespedRequest solicitud) {
        return SesionResponse.de(acceso.iniciarSesion(solicitud.correo(), solicitud.numeroDocumento(),
                solicitud.pin()));
    }

    @PostMapping("/cambio-correo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MensajeResponse solicitarCambioCorreo(@Valid @RequestBody SolicitarCambioCorreoRequest solicitud) {
        acceso.solicitarCambioCorreo(solicitud.tipoDocumento(), solicitud.numeroDocumento(), solicitud.nuevoCorreo());
        return new MensajeResponse("Si los datos corresponden a una cuenta, le enviamos un PIN al teléfono y al correo"
                + " actual. Vence en 15 minutos.");
    }

    @PutMapping("/cambio-correo")
    public MensajeResponse confirmarCambioCorreo(@Valid @RequestBody ConfirmarCambioCorreoRequest solicitud) {
        acceso.confirmarCambioCorreo(solicitud.tipoDocumento(), solicitud.numeroDocumento(), solicitud.pin());
        return new MensajeResponse("El correo de la cuenta quedó cambiado.");
    }
}
