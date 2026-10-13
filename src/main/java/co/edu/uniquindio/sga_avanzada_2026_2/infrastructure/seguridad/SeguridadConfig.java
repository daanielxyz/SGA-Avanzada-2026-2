package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;

/**
 * Seguridad de la API (DEC-67): sin sesión de servidor, cada petición trae su token JWT y Spring Security lo valida
 * y arma el rol; los permisos están en {@link PermisosPorRol}.
 */
@Configuration
@EnableConfigurationProperties(SeguridadPropiedades.class)
public class SeguridadConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SeguridadConfig.class);
    private static final String ALGORITMO = "HmacSHA256";
    private static final int MINIMO_SECRETO = 32;

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, RespuestasDeSeguridad respuestas) throws Exception {
        return http
                // Sin cookies ni sesión no hay nada que un sitio ajeno pueda aprovechar: CSRF no aplica
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(PermisosPorRol::configurar)
                .oauth2ResourceServer(tokens -> tokens
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolDelToken()))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .build();
    }

    /** El dato {@code rol} del token pasa a ser el rol de Spring Security. */
    private static JwtAuthenticationConverter rolDelToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(EmisorTokens.ROL);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(roles);
        return conversor;
    }

    /**
     * Clave con la que se firman y validan los tokens. Sin {@code SGA_JWT_SECRETO} se usa una al azar: sirve para
     * desarrollo, pero los tokens dejan de valer al reiniciar y no se comparten entre varias instancias.
     *
     * @throws IllegalStateException si el secreto configurado tiene menos de 32 caracteres
     */
    @Bean
    public SecretKey claveDeFirma(SeguridadPropiedades propiedades) {
        String secreto = propiedades.jwt().secreto();
        if (secreto == null || secreto.isBlank()) {
            LOG.warn("Falta SGA_JWT_SECRETO: los tokens se firman con una clave al azar que cambia en cada arranque");
            byte[] alAzar = new byte[MINIMO_SECRETO];
            new SecureRandom().nextBytes(alAzar);
            return new SecretKeySpec(alAzar, ALGORITMO);
        }
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MINIMO_SECRETO) {
            throw new IllegalStateException("SGA_JWT_SECRETO debe tener al menos " + MINIMO_SECRETO + " caracteres");
        }
        return new SecretKeySpec(bytes, ALGORITMO);
    }

    @Bean
    public JwtEncoder codificadorDeTokens(SecretKey claveDeFirma) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveDeFirma));
    }

    /** Valida firma y vencimiento, este con el reloj del sistema (EST-06). */
    @Bean
    public JwtDecoder decodificadorDeTokens(SecretKey claveDeFirma, Clock reloj) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveDeFirma)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator vigencia = new JwtTimestampValidator();
        vigencia.setClock(reloj);
        decodificador.setJwtValidator(vigencia);
        return decodificador;
    }

    @Bean
    public PasswordEncoder cifradorDeContrasenas() {
        return new BCryptPasswordEncoder();
    }
}
