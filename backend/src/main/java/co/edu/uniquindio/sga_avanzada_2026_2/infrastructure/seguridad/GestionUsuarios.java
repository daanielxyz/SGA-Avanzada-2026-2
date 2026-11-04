package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoDuplicadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El administrador gestiona al personal interno (CU-06 · USU-03 · USU-05 · ROL-03 · DEC-71). Nunca crea huéspedes: su
 * cuenta es su titular (DEC-68).
 */
@Service
public class GestionUsuarios {

    private final UsuarioRepository usuarios;
    private final GeneradorCodigos generador;
    private final CodigosDelPersonal codigos;
    private final String alojamientoId;

    public GestionUsuarios(UsuarioRepository usuarios, GeneradorCodigos generador, CodigosDelPersonal codigos,
                           @Value("${sga.alojamiento.id:ALO-1}") String alojamientoId) {
        this.usuarios = usuarios;
        this.generador = generador;
        this.codigos = codigos;
        this.alojamientoId = alojamientoId;
    }

    /**
     * Crea el usuario sin contraseña y le envía al correo el código de activación, con el que fija la suya (DEC-71).
     *
     * @throws RecursoDuplicadoException si ya hay un usuario con ese correo (USU-01)
     * @throws ReglaDominioException     si el rol no es interno
     */
    @Transactional
    public UsuarioResult crear(String correo, Rol rol, Usuario.DatosPersonales datos) {
        Correo normalizado = new Correo(correo);
        if (usuarios.findByCorreo(normalizado.valor()).isPresent()) {
            throw new RecursoDuplicadoException("un usuario con el correo", normalizado.valor());
        }
        Usuario usuario = Usuario.crearInterno(new UsuarioId(generador.siguiente(SerieCodigo.USUARIO)),
                alojamientoId, normalizado, rol, datos);
        usuarios.save(usuario);
        codigos.enviar(usuario, CodigoUsuario.Proposito.ACTIVACION);
        return UsuarioResult.de(usuario);
    }

    /** Todos los usuarios, también los eliminados, por nombre, de 10 en 10 (DEC-18). */
    @Transactional(readOnly = true)
    public Pagina<UsuarioResult> listar(int numeroPagina) {
        Page<Usuario> pagina = usuarios.findAllByOrderByNombreAscIdAsc(PageRequest.of(numeroPagina, Pagina.TAMANO));
        return new Pagina<>(pagina.getContent().stream().map(UsuarioResult::de).toList(), numeroPagina,
                Pagina.TAMANO, pagina.getTotalElements());
    }

    /**
     * Cambia los datos y el rol (ROL-03). El correo no cambia (USU-01). Un cambio de rol corta sus sesiones abiertas.
     *
     * @throws RecursoNoEncontradoException si no existe
     * @throws ReglaDominioException        si el rol no es interno, el usuario está eliminado o se le quitaría el
     *                                      rol al último administrador
     */
    @Transactional
    public UsuarioResult modificar(String id, Rol rol, Usuario.DatosPersonales datos) {
        Usuario usuario = buscar(id);
        if (usuario.eliminado()) {
            throw new ReglaDominioException("El usuario " + id + " está eliminado");
        }
        if (rol != Rol.ADMINISTRADOR) {
            exigirOtroAdministrador(usuario);
        }
        usuario.actualizarDatos(datos);
        usuario.cambiarRol(rol);
        usuarios.save(usuario);
        return UsuarioResult.de(usuario);
    }

    /**
     * Eliminación lógica (USU-05): deja de entrar y sus sesiones se cortan. Sus registros, salidas y autorizaciones
     * conservan su id como autor.
     *
     * @throws RecursoNoEncontradoException si no existe
     * @throws ReglaDominioException        si ya estaba eliminado o es el último administrador
     */
    @Transactional
    public UsuarioResult eliminar(String id) {
        Usuario usuario = buscar(id);
        if (usuario.eliminado()) {
            throw new ReglaDominioException("El usuario " + id + " ya está eliminado");
        }
        exigirOtroAdministrador(usuario);
        usuario.eliminar();
        usuarios.save(usuario);
        return UsuarioResult.de(usuario);
    }

    private Usuario buscar(String id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el usuario", id));
    }

    // DEC-71: el alojamiento nunca se queda sin quien lo administre
    private void exigirOtroAdministrador(Usuario usuario) {
        if (usuario.rol() == Rol.ADMINISTRADOR && usuarios.countByRolAndEliminadoFalse(Rol.ADMINISTRADOR) <= 1) {
            throw new ReglaDominioException("Es el último administrador: el alojamiento no puede quedarse sin uno");
        }
    }
}
