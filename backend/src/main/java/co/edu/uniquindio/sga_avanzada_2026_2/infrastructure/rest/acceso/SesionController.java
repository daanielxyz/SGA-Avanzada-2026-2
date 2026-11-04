package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.CerrarSesion;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.IniciarSesion;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de acceso (CU-02 · CU-48): entrar es público, porque aquí se obtiene el token que piden los demás; cerrar
 * la sesión pide ese token.
 */
@RestController
@RequestMapping("/api/auth")
public class SesionController {

    private final IniciarSesion iniciarSesion;
    private final CerrarSesion cerrarSesion;
    private final UsuarioActual usuario;

    public SesionController(IniciarSesion iniciarSesion, CerrarSesion cerrarSesion, UsuarioActual usuario) {
        this.iniciarSesion = iniciarSesion;
        this.cerrarSesion = cerrarSesion;
        this.usuario = usuario;
    }

    @PostMapping("/login")
    public SesionResponse iniciarSesion(@Valid @RequestBody IniciarSesionRequest solicitud) {
        return SesionResponse.de(iniciarSesion.ejecutar(solicitud.correo(), solicitud.contrasena()));
    }

    /** El token de esta petición deja de valer; vale para el personal y para el huésped (DEC-71). */
    @PostMapping("/logout")
    public MensajeResponse cerrarSesion() {
        cerrarSesion.ejecutar(usuario.token());
        return new MensajeResponse("La sesión quedó cerrada.");
    }
}
