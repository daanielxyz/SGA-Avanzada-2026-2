package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.IniciarSesion;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de acceso (CU-02). Es público: aquí se obtiene el token que piden los demás.
 */
@RestController
@RequestMapping("/api/auth")
public class SesionController {

    private final IniciarSesion iniciarSesion;

    public SesionController(IniciarSesion iniciarSesion) {
        this.iniciarSesion = iniciarSesion;
    }

    @PostMapping("/login")
    public SesionResponse iniciarSesion(@Valid @RequestBody IniciarSesionRequest solicitud) {
        return SesionResponse.de(iniciarSesion.ejecutar(solicitud.correo(), solicitud.contrasena()));
    }
}
