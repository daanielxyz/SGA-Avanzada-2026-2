package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Lo que responde la seguridad antes de llegar a un controlador, con el mismo cuerpo de error del resto de la API
 * (DEC-33): sin sesión válida → 401; con sesión pero sin el rol → 403 (DEC-67).
 */
@Component
public class RespuestasDeSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper json;

    public RespuestasDeSeguridad(ObjectMapper json) {
        this.json = json;
    }

    /** Sin token, o con uno vencido, alterado o mal formado. */
    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException e)
            throws IOException {
        respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribir(respuesta, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                "Debe iniciar sesión: falta el token o ya no es válido");
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException e)
            throws IOException {
        escribir(respuesta, HttpStatus.FORBIDDEN, "SIN_PERMISO", "Su rol no permite esta acción");
    }

    private void escribir(HttpServletResponse respuesta, HttpStatus estado, String codigo, String mensaje)
            throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write(
                json.writeValueAsString(new ErrorResponse(estado.value(), codigo, mensaje, List.of())));
    }
}
