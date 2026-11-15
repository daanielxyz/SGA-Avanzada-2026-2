package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.application.asistente.ResponderPreguntaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.AccesoDenegadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * El asistente de dudas del PMS (DEC-127): recepción, servicio y administración preguntan cómo se hace algo y reciben
 * la respuesta con las secciones del manual en que se basa. Siempre responde 200: si la IA no puede, responde el
 * manual y lo dice en {@code origen} y {@code aviso}. Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/asistente")
public class AsistenteController {

    private static final List<Rol> DEL_PERSONAL = List.of(Rol.RECEPCIONISTA, Rol.PERSONAL_SERVICIO, Rol.ADMINISTRADOR);

    private final AlojamientoActual alojamiento;
    private final UsuarioActual usuario;
    private final ResponderPreguntaUseCase responderPregunta;

    public AsistenteController(AlojamientoActual alojamiento, UsuarioActual usuario,
                               ResponderPreguntaUseCase responderPregunta) {
        this.alojamiento = alojamiento;
        this.usuario = usuario;
        this.responderPregunta = responderPregunta;
    }

    @PostMapping("/preguntas")
    public PreguntaResponse preguntar(@Valid @RequestBody PreguntaRequest solicitud, Authentication sesion) {
        return PreguntaResponse.de(responderPregunta.ejecutar(
                solicitud.aCommand(alojamiento.id(), usuario.id(), rolDe(sesion).name())));
    }

    /** El rol de la sesión; {@code PermisosPorRol} ya dejó fuera al huésped, esto es por si cambia la regla. */
    private static Rol rolDe(Authentication sesion) {
        return DEL_PERSONAL.stream()
                .filter(rol -> sesion.getAuthorities().stream()
                        .anyMatch(autoridad -> rol.autoridad().equals(autoridad.getAuthority())))
                .findFirst()
                .orElseThrow(() -> new AccesoDenegadoException("El asistente es solo para el personal del alojamiento"));
    }
}
