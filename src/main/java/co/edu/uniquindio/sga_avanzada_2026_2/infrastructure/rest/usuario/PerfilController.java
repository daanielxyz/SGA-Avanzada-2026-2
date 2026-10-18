package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.usuario;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso.MensajeResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.CuentaPersonal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El perfil del usuario interno que hace la petición (CU-03 · CU-04). El huésped tiene el suyo en
 * {@code /api/mi-cuenta} (DEC-69).
 */
@RestController
@RequestMapping("/api/usuarios/perfil")
public class PerfilController {

    private final UsuarioActual usuario;
    private final CuentaPersonal cuenta;

    public PerfilController(UsuarioActual usuario, CuentaPersonal cuenta) {
        this.usuario = usuario;
        this.cuenta = cuenta;
    }

    @GetMapping
    public UsuarioResponse consultar() {
        return UsuarioResponse.de(cuenta.consultar(usuario.id()));
    }

    @PutMapping
    public UsuarioResponse actualizar(@Valid @RequestBody DatosPersonalesRequest solicitud) {
        return UsuarioResponse.de(cuenta.actualizar(usuario.id(), solicitud.aDatos()));
    }

    /** Las sesiones abiertas, esta incluida, dejan de valer: hay que volver a entrar (DEC-71). */
    @PutMapping("/contrasena")
    public MensajeResponse cambiarContrasena(@Valid @RequestBody CambiarContrasenaRequest solicitud) {
        cuenta.cambiarContrasena(usuario.id(), solicitud.actual(), solicitud.nueva());
        return new MensajeResponse("La contraseña quedó cambiada. Vuelva a iniciar sesión.");
    }
}
