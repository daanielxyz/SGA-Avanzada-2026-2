package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.AvanzarNovedad;
import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.AvanzarNovedadCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.ConsultarNovedades;
import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.RegistrarNovedad;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de las novedades de un apartamento (CU-43 · CU-44 · CU-55). El autor de cada paso es el usuario que hace
 * la petición (DEC-61). Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
public class NovedadController {

    private final UsuarioActual usuario;
    private final RegistrarNovedad registrarNovedad;
    private final AvanzarNovedad avanzarNovedad;
    private final ConsultarNovedades consultarNovedades;

    public NovedadController(UsuarioActual usuario, RegistrarNovedad registrarNovedad,
                             AvanzarNovedad avanzarNovedad, ConsultarNovedades consultarNovedades) {
        this.usuario = usuario;
        this.registrarNovedad = registrarNovedad;
        this.avanzarNovedad = avanzarNovedad;
        this.consultarNovedades = consultarNovedades;
    }

    @PostMapping("/api/apartamentos/{codigo}/novedades")
    @ResponseStatus(HttpStatus.CREATED)
    public NovedadResponse registrar(@PathVariable String codigo,
                                     @Valid @RequestBody RegistrarNovedadRequest solicitud) {
        return NovedadResponse.de(registrarNovedad.ejecutar(solicitud.aCommand(codigo, usuario.id())));
    }

    /** De la más reciente a la más antigua, incluidas las cerradas (NOV-06). */
    @GetMapping("/api/apartamentos/{codigo}/novedades")
    public PaginaResponse<NovedadResponse> consultar(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(consultarNovedades.ejecutar(codigo, pagina), NovedadResponse::de);
    }

    /** Da el siguiente paso: ABIERTA → EN_REVISION → CERRADA. */
    @PutMapping("/api/novedades/{id}/avanzar")
    public NovedadResponse avanzar(@PathVariable String id) {
        return NovedadResponse.de(avanzarNovedad.ejecutar(new AvanzarNovedadCommand(id, usuario.id())));
    }
}
