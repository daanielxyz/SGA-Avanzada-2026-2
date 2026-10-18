package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Lo que un token firmado y vigente todavía debe cumplir en cada petición (DEC-71): que su sesión no se haya cerrado
 * (CU-48) y, si es de un usuario interno, que este siga activo, con el mismo rol y sin haber cambiado la contraseña
 * después. Si no cumple, la petición recibe 401.
 */
@Component
public class ValidadorSesion implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2TokenValidatorResult CERRADA = OAuth2TokenValidatorResult.failure(
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "La sesión ya no es válida", null));

    private final TokenRevocadoRepository revocados;
    private final UsuarioRepository usuarios;

    public ValidadorSesion(TokenRevocadoRepository revocados, UsuarioRepository usuarios) {
        this.revocados = revocados;
        this.usuarios = usuarios;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getId() != null && revocados.existsById(token.getId())) {
            return CERRADA;
        }
        String rol = token.getClaimAsString(EmisorTokens.ROL);
        boolean delPersonal = !Rol.HUESPED.name().equals(rol);
        boolean vale = !delPersonal || usuarios.findById(token.getSubject())
                .filter(usuario -> usuario.aceptaSesion(rol, token.getClaim(EmisorTokens.SESIONES)))
                .isPresent();
        return vale ? OAuth2TokenValidatorResult.success() : CERRADA;
    }
}
