package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.CodigoUsuario;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.RecuperacionContrasena;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El personal interno fija su contraseña con un código del correo, sin sesión: al activar la cuenta y al recuperarla
 * (CU-05 · USU-06 · USU-08 · DEC-71).
 */
@RestController
@RequestMapping("/api/auth")
public class RecuperacionController {

    private final RecuperacionContrasena recuperacion;

    public RecuperacionController(RecuperacionContrasena recuperacion) {
        this.recuperacion = recuperacion;
    }

    /** Responde lo mismo exista o no el correo (USU-08). */
    @PostMapping("/recuperar-contrasena")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MensajeResponse recuperar(@Valid @RequestBody RecuperarContrasenaRequest solicitud) {
        recuperacion.solicitar(solicitud.correo());
        return new MensajeResponse("Si el correo es de un usuario, le enviamos un código. Vence en 15 minutos.");
    }

    @PostMapping("/restablecer-contrasena")
    public MensajeResponse restablecer(@Valid @RequestBody FijarContrasenaRequest solicitud) {
        recuperacion.fijar(solicitud.correo(), CodigoUsuario.Proposito.RECUPERACION, solicitud.codigo(),
                solicitud.contrasena());
        return new MensajeResponse("La contraseña quedó cambiada. Ya puede iniciar sesión.");
    }

    @PostMapping("/activar")
    public MensajeResponse activar(@Valid @RequestBody FijarContrasenaRequest solicitud) {
        recuperacion.fijar(solicitud.correo(), CodigoUsuario.Proposito.ACTIVACION, solicitud.codigo(),
                solicitud.contrasena());
        return new MensajeResponse("La cuenta quedó activada. Ya puede iniciar sesión.");
    }
}
