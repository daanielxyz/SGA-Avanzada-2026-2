package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Min;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ConsultarConflictosPendientesUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ResolverConflictoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints con los que el administrador revisa los conflictos de canal (CONF-04). Resolver uno no cambia ninguna
 * reserva (CONF-02). Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/conflictos")
public class ConflictoController {

    private final UsuarioActual usuario;
    private final ConsultarConflictosPendientesUseCase consultarPendientes;
    private final ResolverConflictoUseCase resolverConflicto;

    public ConflictoController(UsuarioActual usuario, ConsultarConflictosPendientesUseCase consultarPendientes,
                               ResolverConflictoUseCase resolverConflicto) {
        this.usuario = usuario;
        this.consultarPendientes = consultarPendientes;
        this.resolverConflicto = resolverConflicto;
    }

    @GetMapping("/pendientes")
    public PaginaResponse<ConflictoResponse> consultarPendientes(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(Pagina.de(consultarPendientes.ejecutar(), pagina), ConflictoResponse::de);
    }

    @PutMapping("/{id}/resolver")
    public ConflictoResponse resolver(@PathVariable String id,
                                      @Valid @RequestBody ResolverConflictoRequest solicitud) {
        return ConflictoResponse.de(resolverConflicto.ejecutar(solicitud.aCommand(id, usuario.id())));
    }
}
