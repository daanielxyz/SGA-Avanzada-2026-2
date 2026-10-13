package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Al arrancar crea el primer administrador del despliegue con {@code SGA_ADMIN_CORREO} y {@code SGA_ADMIN_CLAVE} si
 * todavía no hay ninguno (DEC-67). Los demás usuarios internos los crea él (USU-03).
 */
@Component
public class InicializadorAdministrador implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorAdministrador.class);

    private final UsuarioRepository usuarios;
    private final GeneradorCodigos codigos;
    private final PasswordEncoder cifrador;
    private final SeguridadPropiedades.Administrador inicial;
    private final String alojamientoId;

    public InicializadorAdministrador(UsuarioRepository usuarios, GeneradorCodigos codigos, PasswordEncoder cifrador,
                                      SeguridadPropiedades propiedades,
                                      @Value("${sga.alojamiento.id:ALO-1}") String alojamientoId) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.cifrador = cifrador;
        this.inicial = propiedades.administrador();
        this.alojamientoId = alojamientoId;
    }

    /**
     * @throws IllegalStateException si la clave configurada no cumple la política de contraseñas (USU-02)
     */
    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (usuarios.existsByRolAndEliminadoFalse(Rol.ADMINISTRADOR)) {
            return;
        }
        if (falta(inicial.correo()) || falta(inicial.clave())) {
            LOG.warn("No hay ningún administrador y faltan SGA_ADMIN_CORREO o SGA_ADMIN_CLAVE: nadie puede entrar");
            return;
        }
        if (!PoliticaContrasena.laCumple(inicial.clave())) {
            throw new IllegalStateException("SGA_ADMIN_CLAVE " + PoliticaContrasena.DESCRIPCION);
        }
        Usuario administrador = Usuario.crear(new UsuarioId(codigos.siguiente(SerieCodigo.USUARIO)), alojamientoId,
                inicial.nombre(), new Correo(inicial.correo()), cifrador.encode(inicial.clave()), Rol.ADMINISTRADOR);
        usuarios.save(administrador);
        LOG.info("Se creó el administrador inicial {} ({})", administrador.id(), administrador.correo());
    }

    private static boolean falta(String valor) {
        return valor == null || valor.isBlank();
    }
}
