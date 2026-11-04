package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * La sesión de las pruebas REST (DEC-67): toda petición del {@code MockMvc} que entrega Spring va como el
 * administrador {@link #ADMINISTRADOR}, que puede todo, para que cada prueba se ocupe de su regla y no de entrar.
 * La que prueba permisos cambia de usuario con {@code .with(como(rol))} o {@code .with(sinSesion())}.
 */
@Component
public class SesionDePrueba implements MockMvcBuilderCustomizer {

    /** Autor de lo que las pruebas hacen sin decir quién. */
    public static final String ADMINISTRADOR = "USR-900";

    @Override
    public void customize(ConfigurableMockMvcBuilder<?> constructor) {
        constructor.defaultRequest(get("/").with(como(Rol.ADMINISTRADOR, ADMINISTRADOR)));
    }

    /** Un usuario con ese rol; su id es {@code USR-} más el nombre del rol. */
    public static RequestPostProcessor como(Rol rol) {
        return como(rol, idDe(rol));
    }

    public static RequestPostProcessor como(Rol rol, String id) {
        return jwt().jwt(token -> token.subject(id).claim(EmisorTokens.ROL, rol.name()))
                .authorities(new SimpleGrantedAuthority(rol.autoridad()));
    }

    public static String idDe(Rol rol) {
        return "USR-" + rol.name();
    }

    public static RequestPostProcessor sinSesion() {
        return anonymous();
    }
}
