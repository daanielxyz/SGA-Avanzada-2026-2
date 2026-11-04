package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data sobre la tabla {@code usuario}. No es un puerto del dominio: el módulo de acceso es infraestructura.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    /** @param correo normalizado, como lo deja {@code Correo} */
    Optional<Usuario> findByCorreo(String correo);

    boolean existsByRolAndEliminadoFalse(Rol rol);

    long countByRolAndEliminadoFalse(Rol rol);

    Page<Usuario> findAllByOrderByNombreAscIdAsc(Pageable pagina);
}
