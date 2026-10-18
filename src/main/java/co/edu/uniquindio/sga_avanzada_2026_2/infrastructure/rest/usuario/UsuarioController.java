package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.usuario;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.GestionUsuarios;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión del personal interno, solo del administrador (CU-06 · DEC-71).
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final GestionUsuarios gestion;

    public UsuarioController(GestionUsuarios gestion) {
        this.gestion = gestion;
    }

    /** Nace sin contraseña: le llega al correo el código para fijarla. Sin Location: no tiene {@code GET} por id. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody CrearUsuarioRequest solicitud) {
        return UsuarioResponse.de(gestion.crear(solicitud.correo(), Rol.valueOf(solicitud.rol()), solicitud.aDatos()));
    }

    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(gestion.listar(pagina), UsuarioResponse::de);
    }

    @PutMapping("/{id}")
    public UsuarioResponse modificar(@PathVariable String id, @Valid @RequestBody ModificarUsuarioRequest solicitud) {
        return UsuarioResponse.de(gestion.modificar(id, Rol.valueOf(solicitud.rol()), solicitud.aDatos()));
    }

    @PutMapping("/{id}/eliminar")
    public UsuarioResponse eliminar(@PathVariable String id) {
        return UsuarioResponse.de(gestion.eliminar(id));
    }
}
